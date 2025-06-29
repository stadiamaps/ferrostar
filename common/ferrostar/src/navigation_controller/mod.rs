//! The navigation state machine.

pub mod models;
pub mod recording;
pub mod step_advance;

#[cfg(test)]
pub(crate) mod test_helpers;

use crate::{
    algorithms::{
        advance_step, apply_snapped_course, calculate_trip_progress,
        index_of_closest_segment_origin, snap_user_location_to_line,
    },
    models::{Route, RouteStep, UserLocation, Waypoint},
    navigation_controller::models::TripSummary,
};
use chrono::Utc;
use geo::{
    algorithm::{Distance, Haversine},
    geometry::{LineString, Point},
};
use models::{
    NavState, NavigationControllerConfig, StepAdvanceStatus, TripState, WaypointAdvanceMode,
};
use std::clone::Clone;
use std::sync::Arc;

#[cfg(feature = "wasm-bindgen")]
use wasm_bindgen::{prelude::wasm_bindgen, JsValue};

/// Manages the navigation lifecycle through a route,
/// returning an updated state given inputs like user location.
///
/// Notes for implementing a new platform:
/// - A controller is bound to a single route; if you want recalculation, create a new instance.
/// - This is a pure type (no interior mutability), so a core function of your platform code is responsibly managing mutable state.
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct NavigationController {
    route: Route,
    config: NavigationControllerConfig,
}

/// Core interface for navigation functionalities.
///
/// This trait defines the essential operations for a navigation state manager.
/// This lets us build additional layers (e.g. event logging)
/// around [`NavigationController`] a composable manner.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub trait Navigator: Send + Sync {
    fn get_initial_state(&self, location: UserLocation) -> NavState;
    fn advance_to_next_step(&self, state: &NavState) -> NavState;
    fn update_user_location(&self, location: UserLocation, state: &NavState) -> NavState;
}

/// Creates a new navigation controller for the given route and configuration.
///
/// It returns an Arc-wrapped trait object implementing `Navigator`.
/// If `should_record` is true, it creates a controller with event recording enabled.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub fn create_navigator(
    route: Route,
    config: NavigationControllerConfig,
    should_record: bool,
) -> Arc<dyn Navigator> {
    if should_record {
        // Creates a navigation controller with a wrapper that records events.
        // TODO: Currently just returns the regular controller
        Arc::new(NavigationController { route, config })
    } else {
        // Creates a normal navigation controller.
        Arc::new(NavigationController { route, config })
    }
}

#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationController {
    #[cfg_attr(feature = "uniffi", uniffi::constructor)]
    /// Create a navigation controller for a route and configuration.
    pub fn new(route: Route, config: NavigationControllerConfig) -> Self {
        Self { route, config }
    }
}

impl Navigator for NavigationController {
    /// Returns initial trip state as if the user had just started the route with no progress.
    fn get_initial_state(&self, location: UserLocation) -> NavState {
        let remaining_steps = self.route.steps.clone();

        let initial_summary = TripSummary {
            distance_traveled: 0.0,
            snapped_distance_traveled: 0.0,
            started_at: Utc::now(),
            ended_at: None,
        };

        let Some(current_route_step) = remaining_steps.first() else {
            // Bail early; if we don't have any steps, this is a useless route
            return NavState::apply_complete(location, initial_summary);
        };

        // TODO: We could move this to the Route struct or NavigationController directly to only calculate it once.
        let current_step_linestring = current_route_step.get_linestring();
        let (current_step_geometry_index, snapped_user_location) =
            self.snap_user_to_line(location, &current_step_linestring);

        let progress = calculate_trip_progress(
            &snapped_user_location.into(),
            &current_step_linestring,
            &remaining_steps,
        );
        let deviation = self.config.route_deviation_tracking.check_route_deviation(
            location,
            &self.route,
            current_route_step,
        );
        let visual_instruction = current_route_step
            .get_active_visual_instruction(progress.distance_to_next_maneuver)
            .cloned();
        let spoken_instruction = current_route_step
            .get_current_spoken_instruction(progress.distance_to_next_maneuver)
            .cloned();

        let annotation_json = current_step_geometry_index
            .and_then(|index| current_route_step.get_annotation_at_current_index(index));

        let trip_state = TripState::Navigating {
            current_step_geometry_index,
            user_location: location,
            snapped_user_location,
            remaining_steps,
            // Skip the first waypoint, as it is the current one
            remaining_waypoints: self.route.waypoints.iter().skip(1).copied().collect(),
            progress,
            summary: initial_summary,
            deviation,
            visual_instruction,
            spoken_instruction,
            annotation_json,
        };
        let next_advance = Arc::clone(&self.config.step_advance_condition);
        return NavState::new(trip_state, next_advance);
    }

    /// Advances navigation to the next step.
    ///
    /// Depending on the advancement strategy, this may be automatic.
    /// For other cases, it is desirable to advance to the next step manually (ex: walking in an
    /// urban tunnel). We leave this decision to the app developer and provide this as a convenience.
    ///
    /// This method is takes the intermediate state (e.g. from `update_user_location`) and advances if necessary.
    /// As a result, you do not to re-calculate things like deviation or the snapped user location (search this file for usage of this function).
    fn advance_to_next_step(&self, state: &NavState) -> NavState {
        match state.trip_state() {
            TripState::Navigating {
                user_location,
                ref remaining_steps,
                ref remaining_waypoints,
                summary,
                ..
            } => {
                let update = advance_step(remaining_steps);
                match update {
                    StepAdvanceStatus::Advanced { step: current_step } => {
                        // Apply the updates
                        let mut remaining_steps = remaining_steps.clone();
                        remaining_steps.remove(0);

                        // Create a new trip state with the updated current_step
                        // and remaining_steps
                        let trip_state = self.create_new_trip_state(
                            &state.trip_state(),
                            &user_location,
                            &current_step,
                            &remaining_steps,
                            &remaining_waypoints,
                        );

                        NavState::new(trip_state, state.step_advance_condition())
                    }
                    StepAdvanceStatus::EndOfRoute => {
                        NavState::completed(user_location, summary.clone())
                    }
                }
            }
            // Pass through any other states.
            _ => state.clone(),
        }
    }

    /// Updates the user's current location and updates the navigation state accordingly.
    ///
    /// # Panics
    ///
    /// If there is no current step ([`TripState::Navigating`] has an empty `remainingSteps` value),
    /// this function will panic.
    fn update_user_location(&self, location: UserLocation, state: &NavState) -> NavState {
        match state.trip_state() {
            TripState::Navigating {
                ref remaining_steps,
                ref remaining_waypoints,
                summary,
                ..
            } => {
                // Remaining steps is empty, the route is finished.
                let Some(current_step) = remaining_steps.first() else {
                    return NavState::apply_complete(location, summary);
                };

                // Trim the remaining waypoints if needed.
                let remaining_waypoints = if self.should_advance_waypoint(&state.trip_state()) {
                    let mut remaining_waypoints = remaining_waypoints.clone();
                    remaining_waypoints.remove(0);
                    remaining_waypoints
                } else {
                    remaining_waypoints.clone()
                };

                // Get the step advance condition result.
                let next_step = remaining_steps.get(1).cloned();
                let step_advance_result = if remaining_steps.len() <= 2 {
                    self.config
                        .arrival_step_advance_condition
                        .should_advance_step(location, current_step.clone(), next_step)
                } else {
                    state.step_advance_condition().should_advance_step(
                        location,
                        current_step.clone(),
                        next_step,
                    )
                };

                let intermediate_nav_state = NavState::new(
                    self.create_new_trip_state(
                        &state.trip_state(),
                        &location,
                        current_step,
                        &remaining_steps,
                        &remaining_waypoints,
                    ),
                    step_advance_result.next_iteration.clone(),
                );

                if step_advance_result.should_advance {
                    // Advance to the next step
                    return self.advance_to_next_step(&intermediate_nav_state);
                }

                return intermediate_nav_state;
            }
            // Pass through
            _ => state.clone(),
        }
    }
}

/// Shared functionality for the navigation controller that is not exported by uniFFI.
impl NavigationController {
    ///
    fn create_new_trip_state(
        &self,
        trip_state: &TripState,
        location: &UserLocation,
        current_step: &RouteStep,
        remaining_steps: &Vec<RouteStep>,
        remaining_waypoints: &Vec<Waypoint>,
    ) -> TripState {
        match trip_state {
            TripState::Navigating {
                user_location: previous_user_location,
                snapped_user_location: previous_snapped_user_location,
                summary: previous_summary,
                ..
            } => {
                //
                // Core navigation logic
                //

                // Find the nearest point on the route line
                let current_step_linestring = current_step.get_linestring();
                let (current_step_geometry_index, snapped_user_location) =
                    self.snap_user_to_line(*location, &current_step_linestring);

                let deviation = self.config.route_deviation_tracking.check_route_deviation(
                    *location,
                    &self.route,
                    current_step,
                );

                // Update trip summary with accumulated distance
                let updated_summary = previous_summary.update(
                    &previous_user_location,
                    &location,
                    &previous_snapped_user_location,
                    &snapped_user_location,
                );

                let progress = calculate_trip_progress(
                    &snapped_user_location.into(),
                    &current_step_linestring,
                    &remaining_steps,
                );

                let visual_instruction = current_step
                    .get_active_visual_instruction(progress.distance_to_next_maneuver)
                    .cloned();
                let spoken_instruction = current_step
                    .get_current_spoken_instruction(progress.distance_to_next_maneuver)
                    .cloned();
                let annotation_json = current_step_geometry_index
                    .and_then(|index| current_step.get_annotation_at_current_index(index));

                TripState::Navigating {
                    current_step_geometry_index: current_step_geometry_index,
                    user_location: location.clone(),
                    snapped_user_location,
                    remaining_steps: remaining_steps.clone(),
                    remaining_waypoints: remaining_waypoints.clone(),
                    progress,
                    summary: updated_summary,
                    deviation,
                    visual_instruction,
                    spoken_instruction,
                    annotation_json,
                }
            }
            _ => trip_state.clone(),
        }
    }

    /// Snaps the user's location to the route line and updates the user's course if necessary.
    ///
    /// This bundles all work related to snapping the user's location to the route line and is not intended to be exported.
    ///
    /// Returns the index of the closest segment origin to the snapped user location as well as the snapped user location.
    fn snap_user_to_line(
        &self,
        location: UserLocation,
        line: &LineString,
    ) -> (Option<u64>, UserLocation) {
        // Snap the user's latitude and longitude to the line.
        let snapped_user_location = snap_user_location_to_line(location, line);

        // Get the index of the closest segment origin to the snapped user location.
        let current_step_geometry_index =
            index_of_closest_segment_origin(snapped_user_location, line);

        // Snap the user's course to the line if the configuration specifies it.
        let snapped_with_course: UserLocation = match &self.config.snapped_location_course_filtering
        {
            models::CourseFiltering::SnapToRoute => {
                apply_snapped_course(snapped_user_location, current_step_geometry_index, line)
            }
            models::CourseFiltering::Raw => snapped_user_location,
        };

        (current_step_geometry_index, snapped_with_course)
    }

    /// Determines if the navigation controller should advance to the next waypoint.
    fn should_advance_waypoint(&self, state: &TripState) -> bool {
        match state {
            TripState::Navigating {
                snapped_user_location,
                ref remaining_waypoints,
                ..
            } => {
                // Update remaining waypoints
                remaining_waypoints.first().is_some_and(|waypoint| {
                    let current_location: Point = snapped_user_location.coordinates.into();
                    let next_waypoint: Point = waypoint.coordinate.into();
                    match self.config.waypoint_advance {
                        WaypointAdvanceMode::WaypointWithinRange(range) => {
                            Haversine.distance(current_location, next_waypoint) < range
                        }
                    }
                })
            }
            _ => false,
        }
    }
}

/// JavaScript wrapper for `NavigationController`.
#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = NavigationController)]
pub struct JsNavigationController(NavigationController);

#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_class = NavigationController)]
impl JsNavigationController {
    #[wasm_bindgen(constructor)]
    pub fn new(route: JsValue, config: JsValue) -> Result<JsNavigationController, JsValue> {
        let route: Route = serde_wasm_bindgen::from_value(route)?;
        let config: NavigationControllerConfig = serde_wasm_bindgen::from_value(config)?;

        Ok(JsNavigationController(NavigationController::new(
            route, config,
        )))
    }

    #[wasm_bindgen(js_name = getInitialState)]
    pub fn get_initial_state(&self, location: JsValue) -> Result<JsValue, JsValue> {
        let location: UserLocation = serde_wasm_bindgen::from_value(location)?;

        serde_wasm_bindgen::to_value(&self.0.get_initial_state(location))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    pub fn advance_to_next_step(&self, state: JsValue) -> Result<JsValue, JsValue> {
        let state: TripState = serde_wasm_bindgen::from_value(state)?;

        serde_wasm_bindgen::to_value(&self.0.advance_to_next_step(&state))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = updateUserLocation)]
    pub fn update_user_location(
        &self,
        location: JsValue,
        state: JsValue,
    ) -> Result<JsValue, JsValue> {
        let location: UserLocation = serde_wasm_bindgen::from_value(location)?;
        let state: TripState = serde_wasm_bindgen::from_value(state)?;

        serde_wasm_bindgen::to_value(&self.0.update_user_location(location, &state))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }
}

#[cfg(test)]
mod tests {
    use super::step_advance::StepAdvanceCondition;
    use super::*;
    use crate::deviation_detection::{RouteDeviation, RouteDeviationTracking};
    use crate::navigation_controller::models::CourseFiltering;
    // use crate::navigation_controller::models::{
    //     CourseFiltering, SpecialAdvanceConditions, StepAdvanceMode,
    // };
    use crate::navigation_controller::step_advance::conditions::{
        DistanceEntryAndExitCondition, DistanceToEndOfStep,
    };
    use crate::navigation_controller::test_helpers::{
        get_test_route, nav_controller_insta_settings, TestRoute,
    };
    // use crate::navigation_controller::test_helpers::{
    //     get_extended_route, get_self_intersecting_route, nav_controller_insta_settings,
    // };
    // use crate::navigation_controller::test_helpers::{get_test_route, TestRoute};
    use crate::simulation::{
        advance_location_simulation, location_simulation_from_route, LocationBias,
    };
    use std::sync::Arc;

    fn test_full_route_state_snapshot(
        route: Route,
        step_advance_condition: Arc<dyn StepAdvanceCondition>,
    ) -> Vec<TripState> {
        let mut simulation_state =
            location_simulation_from_route(&route, Some(10.0), LocationBias::None)
                .expect("Unable to create simulation");

        let controller = NavigationController::new(
            route,
            NavigationControllerConfig {
                waypoint_advance: WaypointAdvanceMode::WaypointWithinRange(100.0),
                // Careful setup: if the user is ever off the route
                // (ex: because of an improper automatic step advance),
                // we want to know about it.
                route_deviation_tracking: RouteDeviationTracking::StaticThreshold {
                    minimum_horizontal_accuracy: 0,
                    max_acceptable_deviation: 0.0,
                },
                snapped_location_course_filtering: CourseFiltering::Raw,
                step_advance_condition,
                arrival_step_advance_condition: Arc::new(DistanceToEndOfStep {
                    distance: 5,
                    minimum_horizontal_accuracy: 0,
                }),
            },
        );

        let mut state = controller.get_initial_state(simulation_state.current_location);
        let mut states = vec![state.clone()];
        loop {
            let new_simulation_state = advance_location_simulation(&simulation_state);
            let new_state =
                controller.update_user_location(new_simulation_state.current_location, &state);

            // // Exit if there are no more locations.
            // // TODO: This test simulation doesn't let us complete the actual route for
            // // step_advance_conditions that require the user to exit the step (e.g. DistanceEntryAndExitCondition)
            // if simulation_state.remaining_locations.is_empty() {
            //     break;
            // }

            match new_state.trip_state() {
                TripState::Idle { .. } => {}
                TripState::Navigating {
                    current_step_geometry_index,
                    ref remaining_steps,
                    ref deviation,
                    ..
                } => {
                    if let Some(index) = current_step_geometry_index {
                        let geom_length = remaining_steps[0].geometry.len() as u64;
                        // Regression test that the geometry index is valid
                        assert!(
                            index < geom_length,
                            "index = {index}, geom_length = {geom_length}"
                        );
                    }

                    // Regression test that we are never marked as off the route.
                    // We used to encounter this with relative step advance on self-intersecting
                    // routes, for example.
                    assert_eq!(deviation, &RouteDeviation::NoDeviation);
                }
                TripState::Complete { .. } => {
                    states.push(new_state);
                    break;
                }
            }

            simulation_state = new_simulation_state;
            state = new_state.clone();
            states.push(new_state);
        }

        states.into_iter().map(|state| state.trip_state()).collect()
    }

    // Full simulations for several routes with different settings

    #[test]
    fn test_extended_exact_distance() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_test_route(TestRoute::Extended),
                Arc::new(DistanceToEndOfStep {
                    distance: 0,
                    minimum_horizontal_accuracy: 0,
                })
            ));
        });
    }

    #[test]
    fn test_extended_relative_linestring() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_test_route(TestRoute::Extended),
                Arc::new(DistanceEntryAndExitCondition::new(0, 0, 0))
            ));
        });
    }

    #[test]
    fn test_self_intersecting_exact_distance() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_test_route(TestRoute::SelfIntersecting),
                Arc::new(DistanceToEndOfStep {
                    distance: 0,
                    minimum_horizontal_accuracy: 0,
                })
            ));
        });
    }

    #[test]
    fn test_self_intersecting_relative_linestring() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_test_route(TestRoute::SelfIntersecting),
                Arc::new(DistanceEntryAndExitCondition::new(0, 0, 0))
            ));
        });
    }

    #[test]
    fn test_self_intersecting_relative_linestring_min_line_distance() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_test_route(TestRoute::SelfIntersecting),
                Arc::new(DistanceToEndOfStep {
                    distance: 0,
                    minimum_horizontal_accuracy: 0,
                })
            ));
        });
    }
}
