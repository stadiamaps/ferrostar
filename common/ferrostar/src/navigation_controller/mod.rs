pub mod models;

#[cfg(test)]
pub(crate) mod test_helpers;

use crate::{
    algorithms::{
        advance_step, distance_to_end_of_step, should_advance_to_next_step,
        snap_user_location_to_line,
    },
    models::{Route, UserLocation},
};
use geo::{HaversineDistance, Point};
use models::*;

/// Manages the navigation lifecycle of a route, reacting to inputs like user location updates
/// and returning a new state.
/// If you want to recalculate a new route, you need to create a new navigation controller.
///
/// In the overall architecture, this is a mid-level construct. It wraps some lower
/// level constructs like the route adapter, but a higher level wrapper handles things
/// like feeding in user location updates, route recalculation behavior, etc.
#[derive(uniffi::Object)]
pub struct NavigationController {
    route: Route,
    config: NavigationControllerConfig,
}

#[uniffi::export]
impl NavigationController {
    #[uniffi::constructor]
    pub fn new(route: Route, config: NavigationControllerConfig) -> Self {
        Self { config, route }
    }

    /// Returns initial trip state as if the user had just started the route with no progress.
    pub fn get_initial_state(&self, location: UserLocation) -> TripState {
        let remaining_steps = self.route.steps.clone();

        let Some(current_route_step) = remaining_steps.first() else {
            // Bail early; if we don't have any steps, this is a useless route
            return TripState::Complete;
        };

        let current_step_linestring = current_route_step.get_linestring();
        let snapped_user_location = snap_user_location_to_line(location, &current_step_linestring);
        let distance_to_next_maneuver =
            distance_to_end_of_step(&snapped_user_location.into(), &current_step_linestring);
        let deviation = self.config.route_deviation_tracking.check_route_deviation(
            location,
            &self.route,
            current_route_step,
        );
        let visual_instruction = current_route_step
            .get_active_visual_instruction(distance_to_next_maneuver)
            .cloned();
        let spoken_instruction = current_route_step
            .get_current_spoken_instruction(distance_to_next_maneuver)
            .cloned();

        TripState::Navigating {
            snapped_user_location,
            remaining_steps: remaining_steps.clone(),
            // Skip the first waypoint, as it is the current one
            remaining_waypoints: self.route.waypoints.iter().skip(1).copied().collect(),
            distance_to_next_maneuver,
            deviation,
            visual_instruction,
            spoken_instruction,
        }
    }

    /// Advances navigation to the next step.
    ///
    /// Depending on the advancement strategy, this may be automatic.
    /// For other cases, it is desirable to advance to the next step manually (ex: walking in an
    /// urban tunnel). We leave this decision to the app developer and provide this as a convenience.
    pub fn advance_to_next_step(&self, state: &TripState) -> TripState {
        match state {
            TripState::Navigating {
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
                            current_location.haversine_distance(&next_waypoint) < 100.0
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

                        let distance_to_next_maneuver =
                            distance_to_end_of_step(&(*snapped_user_location).into(), &linestring);

                        let visual_instruction = current_step
                            .get_active_visual_instruction(distance_to_next_maneuver)
                            .cloned();
                        let spoken_instruction = current_step
                            .get_current_spoken_instruction(distance_to_next_maneuver)
                            .cloned();

                        TripState::Navigating {
                            snapped_user_location: *snapped_user_location,
                            remaining_steps,
                            remaining_waypoints,
                            distance_to_next_maneuver,
                            // NOTE: We *can't* run deviation calculations in this method,
                            // as it requires a non-snapped user location.
                            deviation: *deviation,
                            visual_instruction,
                            spoken_instruction,
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
    pub fn update_user_location(&self, location: UserLocation, state: &TripState) -> TripState {
        match state {
            TripState::Navigating {
                ref remaining_steps,
                ref remaining_waypoints,
                deviation,
                visual_instruction,
                spoken_instruction,
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
                let snapped_user_location =
                    snap_user_location_to_line(location, &current_step_linestring);
                let distance_to_next_maneuver = distance_to_end_of_step(
                    &snapped_user_location.into(),
                    &current_step_linestring,
                );
                let intermediate_state = TripState::Navigating {
                    snapped_user_location,
                    remaining_steps: remaining_steps.clone(),
                    remaining_waypoints: remaining_waypoints.clone(),
                    distance_to_next_maneuver,
                    deviation: *deviation,
                    visual_instruction: visual_instruction.clone(),
                    spoken_instruction: spoken_instruction.clone(),
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
                    TripState::Navigating {
                        snapped_user_location,
                        remaining_steps,
                        remaining_waypoints,
                        distance_to_next_maneuver,
                        deviation: _,
                        visual_instruction: _,
                        spoken_instruction: _,
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
                            .get_active_visual_instruction(distance_to_next_maneuver)
                            .cloned();
                        let spoken_instruction = current_step
                            .get_current_spoken_instruction(distance_to_next_maneuver)
                            .cloned();

                        TripState::Navigating {
                            snapped_user_location,
                            remaining_steps,
                            remaining_waypoints,
                            distance_to_next_maneuver,
                            deviation,
                            visual_instruction,
                            spoken_instruction,
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
