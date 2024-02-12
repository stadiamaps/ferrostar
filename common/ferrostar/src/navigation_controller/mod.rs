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

        TripState::Navigating {
            snapped_user_location,
            remaining_steps: remaining_steps.clone(),
            distance_to_next_maneuver,
            deviation,
        }
    }

    /// Advances navigation to the next step.
    ///
    /// Depending on the advancement strategy, this may be automatic.
    /// For other cases, it is desirable to advance to the next step manually (ex: walking in an
    /// urban tunnel). We leave this decision to the app developer.
    pub fn advance_to_next_step(&self, state: &TripState) -> TripState {
        match state {
            TripState::Navigating {
                snapped_user_location,
                ref remaining_steps,
                deviation,
                ..
            } => {
                let update = advance_step(remaining_steps);
                match update {
                    StepAdvanceStatus::Advanced {
                        step: _,
                        linestring,
                    } => {
                        // Apply the updates
                        let mut remaining_steps = remaining_steps.clone();
                        remaining_steps.remove(0);

                        let distance_to_next_maneuver =
                            distance_to_end_of_step(&(*snapped_user_location).into(), &linestring);
                        TripState::Navigating {
                            snapped_user_location: *snapped_user_location,
                            remaining_steps,
                            distance_to_next_maneuver,
                            deviation: *deviation,
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
                ..
            } => {
                let Some(current_step) = remaining_steps.first() else {
                    return TripState::Complete;
                };

                //
                // Core navigation logic
                //

                // Find the nearest point on the route line
                let mut current_step_linestring = current_step.get_linestring();
                let snapped_user_location =
                    snap_user_location_to_line(location, &current_step_linestring);

                // If on track, update the set of remaining waypoints, remaining steps (drop from the list), and update current step.
                let mut remaining_steps = remaining_steps.clone();
                let current_step = if should_advance_to_next_step(
                    &current_step_linestring,
                    remaining_steps.get(1),
                    &location,
                    self.config.step_advance,
                ) {
                    // Advance to the next step
                    let update = advance_step(&remaining_steps);
                    match update {
                        StepAdvanceStatus::Advanced { step, linestring } => {
                            // Apply the updates
                            // TODO: Figure out an elegant way to factor this out as it appears in two places
                            remaining_steps.remove(0);
                            current_step_linestring = linestring;

                            Some(step.clone())
                        }
                        StepAdvanceStatus::EndOfRoute => {
                            return TripState::Complete;
                        }
                    }
                } else {
                    Some(current_step.clone())
                };

                if let Some(step) = current_step {
                    let distance_to_next_maneuver = distance_to_end_of_step(
                        &snapped_user_location.into(),
                        &current_step_linestring,
                    );

                    let deviation = self.config.route_deviation_tracking.check_route_deviation(
                        location,
                        &self.route,
                        &step,
                    );

                    TripState::Navigating {
                        snapped_user_location,
                        remaining_steps,
                        distance_to_next_maneuver,
                        deviation,
                    }
                } else {
                    TripState::Complete
                }
            }
            // Terminal state
            TripState::Complete => TripState::Complete,
        }
    }
}
