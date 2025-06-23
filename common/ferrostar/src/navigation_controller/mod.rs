//! The navigation state machine.

pub mod models;
pub mod recording;

#[cfg(test)]
pub(crate) mod test_helpers;

use crate::{
    algorithms::{
        advance_step, apply_snapped_course, calculate_trip_progress,
        index_of_closest_segment_origin, should_advance_to_next_step, snap_user_location_to_line,
    },
    models::{Route, UserLocation},
    navigation_controller::models::TripSummary,
};
use chrono::Utc;
use geo::{
    algorithm::{Distance, Haversine},
    geometry::{LineString, Point},
};
use models::{NavigationControllerConfig, StepAdvanceStatus, TripState, WaypointAdvanceMode};
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
/// This trait defines the essential operations for any navigation implementation
/// allowing different navigation strategies (standard, recording)
/// to be used interchangeably
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub trait Navigator: Send + Sync {
    fn get_initial_state(&self, location: UserLocation) -> TripState;
    fn advance_to_next_step(&self, state: &TripState) -> TripState;
    fn update_user_location(&self, location: UserLocation, state: &TripState) -> TripState;
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
        // Creates a navigation controller with recording capabilities.
        // For now, it just returns the regular controller
        Arc::new(NavigationController { route, config })
    } else {
        // Creates a navigation controller without recording capabilities.
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

    /// Returns initial trip state as if the user had just started the route with no progress.
    pub fn get_initial_state(&self, location: UserLocation) -> TripState {
        let remaining_steps = self.route.steps.clone();

        let initial_summary = TripSummary {
            distance_traveled: 0.0,
            snapped_distance_traveled: 0.0,
            started_at: Utc::now(),
            ended_at: None,
        };

        let Some(current_route_step) = remaining_steps.first() else {
            // Bail early; if we don't have any steps, this is a useless route
            return Self::completed_trip_state(location, initial_summary);
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

        TripState::Navigating {
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
        }
    }

    /// Advances navigation to the next step.
    ///
    /// Depending on the advancement strategy, this may be automatic.
    /// For other cases, it is desirable to advance to the next step manually (ex: walking in an
    /// urban tunnel). We leave this decision to the app developer and provide this as a convenience.
    ///
    /// This method is takes the intermediate state (e.g. from `update_user_location`) and advances if necessary.
    /// As a result, you do not to re-calculate things like deviation or the snapped user location (search this file for usage of this function).
    pub fn advance_to_next_step(&self, state: &TripState) -> TripState {
        match state {
            TripState::Idle { user_location } => TripState::Idle {
                user_location: *user_location,
            },
            TripState::Navigating {
                current_step_geometry_index,
                user_location,
                snapped_user_location,
                ref remaining_steps,
                ref remaining_waypoints,
                deviation,
                summary,
                ..
            } => {
                // FIXME: This logic is mostly duplicated below
                let update = advance_step(remaining_steps);
                match update {
                    StepAdvanceStatus::Advanced {
                        step: current_step,
                        linestring,
                    } => {
                        // Apply the updates
                        let mut remaining_steps = remaining_steps.clone();
                        remaining_steps.remove(0);

                        let progress = calculate_trip_progress(
                            &(*snapped_user_location).into(),
                            &linestring,
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
                            current_step_geometry_index: *current_step_geometry_index,
                            user_location: *user_location,
                            snapped_user_location: *snapped_user_location,
                            remaining_steps,
                            remaining_waypoints: remaining_waypoints.clone(),
                            progress,
                            summary: summary.clone(),
                            // NOTE: We *can't* run deviation calculations in this method,
                            // as it requires a non-snapped user location.
                            deviation: *deviation,
                            visual_instruction,
                            spoken_instruction,
                            annotation_json,
                        }
                    }
                    StepAdvanceStatus::EndOfRoute => {
                        Self::completed_trip_state(*user_location, summary.clone())
                    }
                }
            }
            TripState::Complete {
                user_location,
                summary,
            } => Self::completed_trip_state(*user_location, summary.clone()),
        }
    }

    /// Updates the user's current location and updates the navigation state accordingly.
    ///
    /// # Panics
    ///
    /// If there is no current step ([`TripState::Navigating`] has an empty `remainingSteps` value),
    /// this function will panic.
    pub fn update_user_location(&self, location: UserLocation, state: &TripState) -> TripState {
        match state {
            TripState::Idle { .. } => TripState::Idle {
                user_location: Some(location),
            },
            TripState::Navigating {
                ref remaining_steps,
                ref remaining_waypoints,
                deviation,
                visual_instruction,
                spoken_instruction,
                annotation_json,
                summary,
                user_location: previous_user_location,
                snapped_user_location: previous_snapped_user_location,
                ..
            } => {
                let Some(current_step) = remaining_steps.first() else {
                    return Self::completed_trip_state(*previous_user_location, summary.clone());
                };

                //
                // Core navigation logic
                //

                // Find the nearest point on the route line
                let current_step_linestring = current_step.get_linestring();
                let (current_step_geometry_index, snapped_user_location) =
                    self.snap_user_to_line(location, &current_step_linestring);

                // Update trip summary with accumulated distance
                let updated_summary = summary.update(
                    &previous_user_location,
                    &location,
                    &previous_snapped_user_location,
                    &snapped_user_location,
                );

                let progress = calculate_trip_progress(
                    &snapped_user_location.into(),
                    &current_step_linestring,
                    remaining_steps,
                );

                // Trim the remaining waypoints if needed.
                let remaining_waypoints = if self.should_advance_waypoint(state) {
                    let mut remaining_waypoints = remaining_waypoints.clone();
                    remaining_waypoints.remove(0);
                    remaining_waypoints
                } else {
                    remaining_waypoints.clone()
                };

                let intermediate_state = TripState::Navigating {
                    current_step_geometry_index,
                    user_location: location,
                    snapped_user_location,
                    remaining_steps: remaining_steps.clone(),
                    remaining_waypoints: remaining_waypoints.clone(),
                    progress,
                    summary: updated_summary,
                    deviation: *deviation,
                    visual_instruction: visual_instruction.clone(),
                    spoken_instruction: spoken_instruction.clone(),
                    annotation_json: annotation_json.clone(),
                };

                match if should_advance_to_next_step(
                    &current_step_linestring,
                    remaining_steps.get(1),
                    &location,
                    self.config.step_advance,
                ) {
                    // Advance to the next step
                    self.advance_to_next_step(&intermediate_state)
                } else {
                    // Do not advance
                    intermediate_state
                } {
                    TripState::Idle { user_location } => TripState::Idle { user_location },
                    TripState::Navigating {
                        user_location: location,
                        snapped_user_location,
                        remaining_steps,
                        remaining_waypoints,
                        progress,
                        summary,
                        // Explicitly recalculated
                        current_step_geometry_index: _,
                        deviation: _,
                        visual_instruction: _,
                        spoken_instruction: _,
                        annotation_json: _,
                    } => {
                        // Recalculate deviation. This happens later, as the current step may have changed.
                        // The distance to the next maneuver will be updated by advance_to_next_step if needed.
                        let current_step = remaining_steps
                            .first()
                            .expect("Invalid state: navigating with zero remaining steps.");
                        let deviation = self.config.route_deviation_tracking.check_route_deviation(
                            location,
                            &self.route,
                            current_step,
                        );

                        // we need to update the geometry index, since the step has changed
                        let (updated_current_step_geometry_index, updated_snapped_user_location) =
                            if let Some(current_route_step) = remaining_steps.first() {
                                let current_step_linestring = current_route_step.get_linestring();
                                self.snap_user_to_line(
                                    snapped_user_location,
                                    &current_step_linestring,
                                )
                            } else {
                                (current_step_geometry_index, snapped_user_location)
                            };

                        let visual_instruction = current_step
                            .get_active_visual_instruction(progress.distance_to_next_maneuver)
                            .cloned();
                        let spoken_instruction = current_step
                            .get_current_spoken_instruction(progress.distance_to_next_maneuver)
                            .cloned();

                        let annotation_json = current_step_geometry_index
                            .and_then(|index| current_step.get_annotation_at_current_index(index));

                        TripState::Navigating {
                            current_step_geometry_index: updated_current_step_geometry_index,
                            user_location: location,
                            snapped_user_location: updated_snapped_user_location,
                            remaining_steps,
                            remaining_waypoints,
                            progress,
                            summary: summary.clone(),
                            deviation,
                            visual_instruction,
                            spoken_instruction,
                            annotation_json,
                        }
                    }
                    TripState::Complete {
                        user_location,
                        summary,
                    } => Self::completed_trip_state(user_location, summary.clone()),
                }
            }
            // Terminal state
            TripState::Complete {
                user_location,
                summary,
            } => Self::completed_trip_state(*user_location, summary.clone()),
        }
    }
}

/// Shared functionality for the navigation controller that is not exported by uniFFI.
impl NavigationController {
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

    /// Helper function to create a Complete state with the ended_at timestamp set
    fn completed_trip_state(user_location: UserLocation, mut summary: TripSummary) -> TripState {
        summary.ended_at = Some(Utc::now());
        TripState::Complete {
            user_location,
            summary,
        }
    }
}

impl Navigator for NavigationController {
    fn get_initial_state(&self, location: UserLocation) -> TripState {
        self.get_initial_state(location)
    }

    fn advance_to_next_step(&self, state: &TripState) -> TripState {
        self.advance_to_next_step(state)
    }

    fn update_user_location(&self, location: UserLocation, state: &TripState) -> TripState {
        self.update_user_location(location, state)
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
    use super::*;
    use crate::deviation_detection::{RouteDeviation, RouteDeviationTracking};
    use crate::navigation_controller::models::{
        CourseFiltering, SpecialAdvanceConditions, StepAdvanceMode,
    };
    use crate::navigation_controller::test_helpers::{
        get_extended_route, get_self_intersecting_route, nav_controller_insta_settings,
    };
    use crate::simulation::{
        advance_location_simulation, location_simulation_from_route, LocationBias,
    };

    fn test_full_route_state_snapshot(
        route: Route,
        step_advance: StepAdvanceMode,
    ) -> Vec<TripState> {
        let mut simulation_state =
            location_simulation_from_route(&route, Some(10.0), LocationBias::None)
                .expect("Unable to create simulation");

        let controller = NavigationController::new(
            route,
            NavigationControllerConfig {
                waypoint_advance: WaypointAdvanceMode::WaypointWithinRange(100.0),
                // NOTE: We will use a few varieties here via parameterized testing,
                // but the point of this test is *not* testing the thresholds.
                step_advance,
                // Careful setup: if the user is ever off the route
                // (ex: because of an improper automatic step advance),
                // we want to know about it.
                route_deviation_tracking: RouteDeviationTracking::StaticThreshold {
                    minimum_horizontal_accuracy: 0,
                    max_acceptable_deviation: 0.0,
                },
                snapped_location_course_filtering: CourseFiltering::Raw,
            },
        );

        let mut state = controller.get_initial_state(simulation_state.current_location);
        let mut states = vec![state.clone()];
        loop {
            let new_simulation_state = advance_location_simulation(&simulation_state);
            let new_state =
                controller.update_user_location(new_simulation_state.current_location, &state);

            match new_state {
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

        states
    }

    // Full simulations for several routes with different settings

    #[test]
    fn test_extended_exact_distance() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_extended_route(),
                StepAdvanceMode::DistanceToEndOfStep {
                    distance: 0,
                    minimum_horizontal_accuracy: 0,
                }
            ));
        });
    }

    #[test]
    fn test_extended_relative_linestring() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_extended_route(),
                StepAdvanceMode::RelativeLineStringDistance {
                    minimum_horizontal_accuracy: 0,
                    special_advance_conditions: None,
                }
            ));
        });
    }

    #[test]
    fn test_self_intersecting_exact_distance() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_self_intersecting_route(),
                StepAdvanceMode::DistanceToEndOfStep {
                    distance: 0,
                    minimum_horizontal_accuracy: 0,
                }
            ));
        });
    }

    #[test]
    fn test_self_intersecting_relative_linestring() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_self_intersecting_route(),
                StepAdvanceMode::RelativeLineStringDistance {
                    minimum_horizontal_accuracy: 0,
                    special_advance_conditions: None,
                }
            ));
        });
    }

    #[test]
    fn test_self_intersecting_relative_linestring_min_line_distance() {
        nav_controller_insta_settings().bind(|| {
            insta::assert_yaml_snapshot!(test_full_route_state_snapshot(
                get_self_intersecting_route(),
                StepAdvanceMode::RelativeLineStringDistance {
                    minimum_horizontal_accuracy: 0,
                    special_advance_conditions: Some(
                        SpecialAdvanceConditions::MinimumDistanceFromCurrentStepLine(10)
                    ),
                }
            ));
        });
    }
}
