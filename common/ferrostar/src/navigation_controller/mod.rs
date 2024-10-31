//! The navigation state machine.

pub mod models;

#[cfg(test)]
pub(crate) mod test_helpers;

use crate::{
    algorithms::{
        advance_step, apply_snapped_course, calculate_trip_progress,
        index_of_closest_segment_origin, should_advance_to_next_step, snap_user_location_to_line,
    },
    models::{Route, UserLocation},
};
use geo::{
    algorithm::{Distance, Haversine},
    geometry::{LineString, Point},
};
use models::{NavigationControllerConfig, StepAdvanceStatus, TripState};
use std::clone::Clone;

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

        let Some(current_route_step) = remaining_steps.first() else {
            // Bail early; if we don't have any steps, this is a useless route
            return TripState::Complete;
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
            snapped_user_location,
            remaining_steps,
            // Skip the first waypoint, as it is the current one
            remaining_waypoints: self.route.waypoints.iter().skip(1).copied().collect(),
            progress,
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
            TripState::Idle => TripState::Idle,
            TripState::Navigating {
                current_step_geometry_index,
                snapped_user_location,
                ref remaining_steps,
                ref remaining_waypoints,
                deviation,
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

                        // Update remaining waypoints
                        let should_advance_waypoint = if let Some(waypoint) =
                            remaining_waypoints.first()
                        {
                            let current_location: Point = snapped_user_location.coordinates.into();
                            let next_waypoint: Point = waypoint.coordinate.into();
                            // TODO: This is just a hard-coded threshold for the time being.
                            // More sophisticated behavior will take some time and use cases, so punting on this for now.
                            Haversine::distance(current_location, next_waypoint) < 100.0
                        } else {
                            false
                        };

                        let remaining_waypoints = if should_advance_waypoint {
                            let mut remaining_waypoints = remaining_waypoints.clone();
                            remaining_waypoints.remove(0);
                            remaining_waypoints
                        } else {
                            remaining_waypoints.clone()
                        };

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
                            snapped_user_location: *snapped_user_location,
                            remaining_steps,
                            remaining_waypoints,
                            progress,
                            // NOTE: We *can't* run deviation calculations in this method,
                            // as it requires a non-snapped user location.
                            deviation: *deviation,
                            visual_instruction,
                            spoken_instruction,
                            annotation_json,
                        }
                    }
                    StepAdvanceStatus::EndOfRoute => TripState::Complete,
                }
            }
            // It's tempting to throw an error here, since the caller should know better, but
            // a mistake like this is technically harmless.
            TripState::Complete => TripState::Complete,
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
            TripState::Idle => TripState::Idle,
            TripState::Navigating {
                ref remaining_steps,
                ref remaining_waypoints,
                deviation,
                visual_instruction,
                spoken_instruction,
                annotation_json,
                ..
            } => {
                let Some(current_step) = remaining_steps.first() else {
                    return TripState::Complete;
                };

                //
                // Core navigation logic
                //

                // Find the nearest point on the route line
                let current_step_linestring = current_step.get_linestring();
                let (current_step_geometry_index, snapped_user_location) =
                    self.snap_user_to_line(location, &current_step_linestring);

                let progress = calculate_trip_progress(
                    &snapped_user_location.into(),
                    &current_step_linestring,
                    remaining_steps,
                );
                let intermediate_state = TripState::Navigating {
                    current_step_geometry_index,
                    snapped_user_location,
                    remaining_steps: remaining_steps.clone(),
                    remaining_waypoints: remaining_waypoints.clone(),
                    progress,
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
                    TripState::Idle => TripState::Idle,
                    TripState::Navigating {
                        snapped_user_location,
                        remaining_steps,
                        remaining_waypoints,
                        progress,
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
                            snapped_user_location,
                            remaining_steps,
                            remaining_waypoints,
                            progress,
                            deviation,
                            visual_instruction,
                            spoken_instruction,
                            annotation_json,
                        }
                    }
                    TripState::Complete => TripState::Complete,
                }
            }
            // Terminal state
            TripState::Complete => TripState::Complete,
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
