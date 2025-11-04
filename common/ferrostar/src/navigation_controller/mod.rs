//! The navigation state machine.

pub mod models;
pub mod step_advance;
pub mod waypoint_advance;

#[cfg(test)]
pub(crate) mod test_helpers;

#[cfg(feature = "wasm-bindgen")]
use crate::navigation_controller::models::{
    SerializableNavState, SerializableNavigationControllerConfig,
};
use crate::{
    algorithms::{
        advance_step, apply_snapped_course, calculate_trip_progress,
        index_of_closest_segment_origin, snap_user_location_to_line,
    },
    deviation_detection::RouteDeviation,
    models::{Route, RouteStep, UserLocation, Waypoint},
    navigation_controller::{
        models::TripSummary,
        waypoint_advance::{WaypointAdvanceChecker, WaypointAdvanceResult, WaypointCheckEvent},
    },
    navigation_session::{NavigationObserver, NavigationSession, recording::NavigationRecorder},
};
use chrono::Utc;
use geo::geometry::LineString;
use models::{NavState, NavigationControllerConfig, StepAdvanceStatus, TripState};
use std::clone::Clone;
use std::sync::Arc;
#[cfg(feature = "wasm-bindgen")]
use wasm_bindgen::{JsValue, prelude::wasm_bindgen};

/// Core interface for navigation functionalities.
///
/// This trait defines the essential operations for a navigation state manager.
/// This lets us build additional layers (e.g. event logging)
/// around [`NavigationController`] in a composable manner.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub trait Navigator: Send + Sync {
    fn route(&self) -> Route;
    fn get_initial_state(&self, location: UserLocation) -> NavState;
    fn advance_to_next_step(&self, state: NavState) -> NavState;
    fn update_user_location(&self, location: UserLocation, state: NavState) -> NavState;
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
    let observers: Vec<Arc<dyn NavigationObserver>> = if should_record {
        vec![Arc::new(NavigationRecorder::new(
            route.clone(),
            config.clone(),
        ))]
    } else {
        vec![]
    };

    // Creates a normal navigation controller.
    Arc::new(NavigationSession::new(
        Arc::new(NavigationController::new(route, config)),
        observers,
    ))
}

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

#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationController {
    #[cfg_attr(feature = "uniffi", uniffi::constructor)]
    /// Create a navigation controller for a route and configuration.
    pub fn new(route: Route, config: NavigationControllerConfig) -> Self {
        Self { route, config }
    }
}

impl Navigator for NavigationController {
    /// The route associated with this controller.
    fn route(&self) -> Route {
        self.route.clone()
    }

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
            return NavState::complete(location, initial_summary);
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
            remaining_waypoints: self.route.waypoints.iter().skip(1).cloned().collect(),
            progress,
            summary: initial_summary,
            deviation,
            visual_instruction,
            spoken_instruction,
            annotation_json,
        };
        let next_advance = Arc::clone(&self.config.step_advance_condition);
        NavState::new(trip_state, next_advance)
    }

    /// Advances navigation to the next step (or finishes the route).
    ///
    /// Depending on the advancement strategy, this may be automatic.
    /// For other cases, it is desirable to advance to the next step manually (ex: walking in an
    /// urban tunnel). We leave this decision to the app developer and provide this as a convenience.
    ///
    /// This method takes the intermediate state (e.g., from `update_user_location`) and advances if necessary,
    /// and does not handle anything like snapping.
    fn advance_to_next_step(&self, state: NavState) -> NavState {
        match state.trip_state() {
            TripState::Navigating {
                user_location,
                ref remaining_steps,
                ref remaining_waypoints,
                deviation,
                summary,
                ..
            } => {
                let update = advance_step(remaining_steps);
                match update {
                    StepAdvanceStatus::Advanced { step: current_step } => {
                        // Trim the remaining waypoints if needed.
                        let waypoints_result = self.get_new_waypoints(
                            &state.trip_state(),
                            WaypointCheckEvent::StepAdvanced(current_step.clone()),
                        );
                        let remaining_waypoints = match waypoints_result {
                            WaypointAdvanceResult::Unchanged => remaining_waypoints.clone(),
                            WaypointAdvanceResult::Changed(new_waypoints) => new_waypoints,
                        };

                        // Apply the updates
                        let mut remaining_steps = remaining_steps.clone();
                        remaining_steps.remove(0);

                        // Create a new trip state with the updated current_step
                        // and remaining_steps
                        let trip_state = self.create_intermediate_trip_state(
                            state.trip_state(),
                            user_location,
                            current_step,
                            remaining_steps,
                            remaining_waypoints,
                            deviation,
                        );

                        NavState::new(trip_state, state.step_advance_condition())
                    }
                    StepAdvanceStatus::EndOfRoute => NavState::complete(user_location, summary),
                }
            }
            // Pass through
            TripState::Idle { .. } | TripState::Complete { .. } => state.clone(),
        }
    }

    /// Updates the user's current location and updates the navigation state accordingly.
    ///
    /// # Panics
    ///
    /// If there is no current step ([`TripState::Navigating`] has an empty `remainingSteps` value),
    /// this function will panic.
    fn update_user_location(&self, location: UserLocation, state: NavState) -> NavState {
        match state.trip_state() {
            TripState::Navigating {
                remaining_steps,
                ref remaining_waypoints,
                summary,
                ..
            } => {
                // Remaining steps is empty, the route is finished.
                let Some(current_step) = remaining_steps.first().cloned() else {
                    return NavState::complete(location, summary);
                };

                // Trim the remaining waypoints if needed.
                let waypoints_result = self
                    .get_new_waypoints(&state.trip_state(), WaypointCheckEvent::LocationUpdated);
                let remaining_waypoints = match waypoints_result {
                    WaypointAdvanceResult::Unchanged => remaining_waypoints.clone(),
                    WaypointAdvanceResult::Changed(new_waypoints) => new_waypoints,
                };

                let deviation = self.config.route_deviation_tracking.check_route_deviation(
                    location,
                    &self.route,
                    &current_step,
                );

                let is_arriving = remaining_steps.len() <= 2;
                let intermediate_trip_state = self.create_intermediate_trip_state(
                    state.trip_state(),
                    location,
                    current_step,
                    remaining_steps,
                    remaining_waypoints,
                    deviation,
                );

                // Get the step advance condition result.
                let step_advance_result = if is_arriving {
                    self.config
                        .arrival_step_advance_condition
                        .should_advance_step(intermediate_trip_state.clone())
                } else {
                    state
                        .step_advance_condition()
                        .should_advance_step(intermediate_trip_state.clone())
                };

                let should_advance = step_advance_result.should_advance();
                let intermediate_nav_state =
                    NavState::new(intermediate_trip_state, step_advance_result.next_iteration);

                if should_advance {
                    // Advance to the next step
                    let updated_state = self.advance_to_next_step(intermediate_nav_state);

                    return if is_arriving {
                        updated_state
                    } else {
                        // Recurse ("speed run" behavior)
                        self.update_user_location(location, updated_state)
                    };
                }

                intermediate_nav_state
            }
            // Pass through
            TripState::Idle { .. } | TripState::Complete { .. } => state.clone(),
        }
    }
}

// Shared functionality for the navigation controller that is not exported by `UniFFI`.
impl NavigationController {
    /// Create an intermediate trip state with updated values,
    /// but does _not_ advance to the next step or handle arrival.
    ///
    /// Parameters:
    /// - `trip_state`: The existing/last trip state.
    /// - `location`: The user's current location.
    /// - `current_step`: The current route step.
    /// - `remaining_steps`: The remaining route steps.
    /// - `remaining_waypoints`: The remaining waypoints.
    ///
    /// Returns:
    /// - `TripState`: The intermediate trip state.
    fn create_intermediate_trip_state(
        &self,
        trip_state: TripState,
        current_user_location: UserLocation,
        current_step: RouteStep,
        remaining_steps: Vec<RouteStep>,
        remaining_waypoints: Vec<Waypoint>,
        deviation: RouteDeviation,
    ) -> TripState {
        match trip_state {
            TripState::Navigating {
                user_location: previous_user_location,
                snapped_user_location: previous_snapped_user_location,
                summary: previous_summary,
                ..
            } => {
                // Find the nearest point on the route line
                let current_step_linestring = current_step.get_linestring();
                let (current_step_geometry_index, snapped_user_location) =
                    self.snap_user_to_line(current_user_location, &current_step_linestring);

                // Update trip summary with accumulated distance
                let updated_summary = previous_summary.update(
                    &previous_user_location,
                    &current_user_location,
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
                    current_step_geometry_index,
                    user_location: current_user_location,
                    snapped_user_location,
                    remaining_steps,
                    remaining_waypoints,
                    progress,
                    summary: updated_summary,
                    deviation,
                    visual_instruction,
                    spoken_instruction,
                    annotation_json,
                }
            }
            // Pass through
            TripState::Idle { .. } | TripState::Complete { .. } => trip_state,
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

    /// Process waypoint advance
    fn get_new_waypoints(
        &self,
        state: &TripState,
        event: WaypointCheckEvent,
    ) -> WaypointAdvanceResult {
        let checker = WaypointAdvanceChecker {
            mode: self.config.waypoint_advance,
        };
        checker.get_new_waypoints(state, event)
    }
}

/// JavaScript wrapper for `NavigationController`.
/// This wrapper is required because `NavigationController` cannot be directly converted to a JavaScript object
/// and requires serialization/deserialization of its methods' inputs and outputs.
#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = NavigationController)]
pub struct JsNavigationController(Arc<dyn Navigator>);

#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_class = NavigationController)]
impl JsNavigationController {
    #[wasm_bindgen(constructor)]
    pub fn new(
        route: JsValue,
        config: JsValue,
        should_record: JsValue,
    ) -> Result<JsNavigationController, JsValue> {
        let route: Route = serde_wasm_bindgen::from_value(route)?;
        let config: SerializableNavigationControllerConfig =
            serde_wasm_bindgen::from_value(config)?;
        let should_record: bool = serde_wasm_bindgen::from_value(should_record)?;

        Ok(JsNavigationController(create_navigator(
            route,
            config.into(),
            should_record,
        )))
    }

    #[wasm_bindgen(js_name = getInitialState)]
    pub fn get_initial_state(&self, location: JsValue) -> Result<JsValue, JsValue> {
        let location: UserLocation = serde_wasm_bindgen::from_value(location)?;
        let nav_state = self.0.get_initial_state(location);
        let result: SerializableNavState = nav_state.into();

        serde_wasm_bindgen::to_value(&result).map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = advanceToNextStep)]
    pub fn advance_to_next_step(&self, state: JsValue) -> Result<JsValue, JsValue> {
        let state: SerializableNavState = serde_wasm_bindgen::from_value(state)?;
        let new_state = self.0.advance_to_next_step(state.into());

        serde_wasm_bindgen::to_value(&SerializableNavState::from(new_state))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = updateUserLocation)]
    pub fn update_user_location(
        &self,
        location: JsValue,
        state: JsValue,
    ) -> Result<JsValue, JsValue> {
        let location: UserLocation = serde_wasm_bindgen::from_value(location)?;
        let state: SerializableNavState = serde_wasm_bindgen::from_value(state)?;
        let new_state = self.0.update_user_location(location, state.into());

        serde_wasm_bindgen::to_value(&SerializableNavState::from(new_state))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }
}

#[cfg(test)]
mod tests {
    use super::step_advance::StepAdvanceCondition;
    use super::*;
    use crate::deviation_detection::RouteDeviation;
    use crate::navigation_controller::step_advance::conditions::{
        DistanceEntryAndExitCondition, DistanceToEndOfStepCondition,
    };
    use crate::navigation_controller::test_helpers::{
        TestRoute, get_test_navigation_controller_config, get_test_route,
        nav_controller_insta_settings,
    };
    use crate::routing_adapters::osrm::models::OsrmWaypointProperties;
    use crate::simulation::{
        LocationBias, advance_location_simulation, location_simulation_from_route,
    };
    use crate::test_utils::redact_properties;
    use std::sync::Arc;

    fn test_full_route_state_snapshot(
        route: Route,
        step_advance_condition: Arc<dyn StepAdvanceCondition>,
        should_record: bool,
    ) -> (Arc<dyn Navigator>, Vec<NavState>) {
        let mut simulation_state =
            location_simulation_from_route(&route, Some(10.0), LocationBias::None)
                .expect("Unable to create simulation");

        let controller = create_navigator(
            route,
            get_test_navigation_controller_config(step_advance_condition),
            should_record,
        );

        let mut state = controller.get_initial_state(simulation_state.current_location);
        let mut states = vec![state.clone()];
        loop {
            let new_simulation_state = advance_location_simulation(&simulation_state);
            let new_state =
                controller.update_user_location(new_simulation_state.current_location, state);

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

        (controller, states)
    }

    // Full simulations for several routes with different settings

    #[test]
    fn test_extended_exact_distance() {
        nav_controller_insta_settings().bind(|| {
            let (_, states) = test_full_route_state_snapshot(
                get_test_route(TestRoute::Extended),
                Arc::new(DistanceToEndOfStepCondition {
                    distance: 0,
                    minimum_horizontal_accuracy: 0,
                }),
                false,
            );
            insta::assert_yaml_snapshot!(states
                .into_iter()
                .map(|state| state.trip_state())
                .collect::<Vec<_>>(), {
                    ".**.remaining_waypoints[].properties" => insta::dynamic_redaction(redact_properties::<OsrmWaypointProperties>),
                });
        });
    }

    #[test]
    fn test_extended_relative_linestring() {
        nav_controller_insta_settings().bind(|| {
            let (_, states) = test_full_route_state_snapshot(
                get_test_route(TestRoute::Extended),
                Arc::new(DistanceEntryAndExitCondition::exact()),
                false,
            );
            insta::assert_yaml_snapshot!(states
                .into_iter()
                .map(|state| state.trip_state())
                .collect::<Vec<_>>(), {
                    ".**.remaining_waypoints[].properties" => insta::dynamic_redaction(redact_properties::<OsrmWaypointProperties>),
                });
        });
    }

    #[test]
    fn test_self_intersecting_exact_distance() {
        nav_controller_insta_settings().bind(|| {
            let (_, states) = test_full_route_state_snapshot(
                get_test_route(TestRoute::SelfIntersecting),
                Arc::new(DistanceToEndOfStepCondition {
                    distance: 0,
                    minimum_horizontal_accuracy: 0,
                }),
                false,
            );
            insta::assert_yaml_snapshot!(states
                .into_iter()
                .map(|state| state.trip_state())
                .collect::<Vec<_>>(), {
                    ".**.remaining_waypoints[].properties" => insta::dynamic_redaction(redact_properties::<OsrmWaypointProperties>),
                });
        });
    }

    #[test]
    fn test_self_intersecting_relative_linestring() {
        nav_controller_insta_settings().bind(|| {
            let (_, states) = test_full_route_state_snapshot(
                get_test_route(TestRoute::SelfIntersecting),
                Arc::new(DistanceEntryAndExitCondition::exact()),
                false,
            );
            insta::assert_yaml_snapshot!(states
                .into_iter()
                .map(|state| state.trip_state())
                .collect::<Vec<_>>(), {
                    ".**.remaining_waypoints[].properties" => insta::dynamic_redaction(redact_properties::<OsrmWaypointProperties>),
                });
        });
    }

    #[test]
    fn test_self_intersecting_relative_linestring_min_line_distance() {
        nav_controller_insta_settings().bind(|| {
            let (_, states) = test_full_route_state_snapshot(
                get_test_route(TestRoute::SelfIntersecting),
                Arc::new(DistanceToEndOfStepCondition {
                    distance: 0,
                    minimum_horizontal_accuracy: 0,
                }),
                false,
            );
            insta::assert_yaml_snapshot!(states
                .into_iter()
                .map(|state| state.trip_state())
                .collect::<Vec<_>>(), {
                    ".**.remaining_waypoints[].properties" => insta::dynamic_redaction(redact_properties::<OsrmWaypointProperties>),
                });
        });
    }
}
