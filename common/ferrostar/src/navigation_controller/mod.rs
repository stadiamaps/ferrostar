mod algorithms;
pub mod models;

use crate::models::{Route, UserLocation};
use crate::navigation_controller::algorithms::{
    advance_step, distance_to_end_of_step, should_advance_to_next_step,
};
use algorithms::snap_user_location_to_line;
use geo::Coord;
use models::*;
use std::sync::{Arc, Mutex};

/// Manages the navigation lifecycle of a single trip, requesting the initial route and updating
/// internal state based on inputs like user location updates.
///
/// By "navigation lifecycle of a single trip", we mean that this controller comes into being when
/// one (or more) routes have already been calculated and a route has been selected from the
/// alternatives (if applicable). It ends when the user either 1) expresses their intent to cancel
/// the navigation, or 2) they successfully visit all waypoints. This puts a clear bound on the
/// life of this controller, though higher level constructs may live longer.
///
/// In the grand scheme of the architecture, this is a mid-level construct. It wraps some lower
/// level constructs like the route adapter, but a higher level wrapper handles things
/// like feeding in user location updates, route recalculation behavior, etc.
///
/// This is intentionally impossible to construct without a user location, so tasks like
/// waiting for a fix (or determining if a cached fix is good enough), are left to higher levels.
#[derive(uniffi::Object)]
pub struct NavigationController {
    /// The last known location of the user. For all intents and purposes, the "user" is assumed
    /// to be at the location reported by their device (phone, car, etc.)
    ///
    /// NOTE: [Mutex] is used because UniFFI doesn't handle mutating struct operations
    /// very well. Others like [core::cell::RefCell] are not enough as the entire object is required to be both
    /// [Send] and [Sync], and [core::cell::RefCell] is explicitly `!Sync`.
    state: Mutex<TripState>,
    config: NavigationControllerConfig,
}

#[uniffi::export]
impl NavigationController {
    /// Creates a new trip navigation controller given the user's last known location and a route.
    #[uniffi::constructor]
    pub fn new(
        last_user_location: UserLocation,
        route: Route,
        config: NavigationControllerConfig,
    ) -> Arc<Self> {
        let remaining_waypoints = route.waypoints.clone();
        let remaining_steps = route.steps.clone();
        let route_linestring = route
            .geometry
            .iter()
            .map(|c| Coord { x: c.lng, y: c.lat })
            .collect();

        let Some(current_route_step) = remaining_steps.first() else {
            // Bail early; if we don't have any steps, this is a useless route
            return Arc::new(Self {
                state: Mutex::new(TripState::Complete),
                config,
            });
        };

        let current_step_linestring = current_route_step.get_linestring();

        Arc::new(Self {
            state: Mutex::new(TripState::Navigating {
                last_user_location,
                snapped_user_location: snap_user_location_to_line(
                    last_user_location,
                    &current_step_linestring,
                ),
                route,
                route_linestring,
                remaining_waypoints,
                remaining_steps,
                current_step_linestring,
            }),
            config,
        })
    }

    /// Advances navigation to the next step.
    ///
    /// Depending on the advancement strategy, this may be automatic.
    /// For other cases, it is desirable to advance to the next step manually (ex: walking in an
    /// urban tunnel). We leave this decision to the app developer.
    pub fn advance_to_next_step(&self) -> NavigationStateUpdate {
        match self.state.lock() {
            Ok(mut guard) => {
                match *guard {
                    TripState::Navigating {
                        snapped_user_location,
                        ref remaining_waypoints,
                        ref mut remaining_steps,
                        ref mut current_step_linestring,
                        ..
                    } => {
                        let update = advance_step(remaining_steps);
                        // TODO: Anything with remaining_waypoints?
                        match update {
                            StepAdvanceStatus::Advanced { step, linestring } => {
                                // Apply the updates
                                // TODO: Figure out an elegant way to factor this out as it appears in two places
                                remaining_steps.remove(0);
                                *current_step_linestring = linestring;

                                // TODO: Compute this
                                let distance_to_next_maneuver = step.distance;
                                NavigationStateUpdate::Navigating {
                                    snapped_user_location,
                                    remaining_waypoints: remaining_waypoints.clone(),
                                    current_step: step.clone(),
                                    distance_to_next_maneuver,
                                }
                            }
                            StepAdvanceStatus::EndOfRoute => {
                                *guard = TripState::Complete;
                                NavigationStateUpdate::Arrived
                            }
                        }
                    }
                    // It's tempting to throw an error here, since the caller should know better, but
                    // a mistake like this is technically harmless.
                    TripState::Complete => NavigationStateUpdate::Arrived,
                }
            }
            Err(_) => {
                // The only way the mutex can become poisoned is if another caller panicked while
                // holding the mutex. In which case, there is no point in continuing.
                unreachable!("Poisoned mutex. This should never happen.");
            }
        }
    }

    /// Updates the user's current location and updates the navigation state accordingly.
    pub fn update_user_location(&self, location: UserLocation) -> NavigationStateUpdate {
        match self.state.lock() {
            Ok(mut guard) => {
                match *guard {
                    TripState::Navigating {
                        mut last_user_location,
                        mut snapped_user_location,
                        ref remaining_waypoints,
                        ref mut remaining_steps,
                        ref mut current_step_linestring,
                        ..
                    } => {
                        last_user_location = location;

                        let Some(current_step) = remaining_steps.first() else {
                            return NavigationStateUpdate::Arrived;
                        };

                        //
                        // Core navigation logic
                        //

                        // Find the nearest point on the route line
                        snapped_user_location =
                            snap_user_location_to_line(location, current_step_linestring);

                        // TODO: Check if the user's distance is > some configurable threshold, accounting for GPS error, mode of travel, etc.
                        // TODO: If so, flag that the user is off route so higher levels can recalculate if desired

                        // TODO: If on track, update the set of remaining waypoints, remaining steps (drop from the list), and update current step.
                        // IIUC these should always appear within the route itself, which simplifies the logic a bit.
                        // TBD: Do we want to support disjoint routes?
                        // TBD: Do we even need this? I'm still a bit fuzzy on the use cases TBH.
                        let remaining_waypoints = remaining_waypoints.clone();

                        let current_step = if should_advance_to_next_step(
                            current_step_linestring,
                            remaining_steps.get(1),
                            &last_user_location,
                            self.config.step_advance,
                        ) {
                            // Advance to the next step
                            let update = advance_step(remaining_steps);
                            match update {
                                StepAdvanceStatus::Advanced { step, linestring } => {
                                    // Apply the updates
                                    // TODO: Figure out an elegant way to factor this out as it appears in two places
                                    remaining_steps.remove(0);
                                    *current_step_linestring = linestring;

                                    Some(step.clone())
                                }
                                StepAdvanceStatus::EndOfRoute => {
                                    *guard = TripState::Complete;
                                    return NavigationStateUpdate::Arrived;
                                }
                            }
                        } else {
                            Some(current_step.clone())
                        };

                        // TODO: Calculate distance to the next step
                        // Hmm... We don't currently store the LineString for the current step...
                        // let fraction_along_line = route_linestring.line_locate_point(&point!(x: snapped_user_location.coordinates.lng, y: snapped_user_location.coordinates.lat));

                        if let Some(step) = current_step {
                            let distance_to_next_maneuver = distance_to_end_of_step(
                                &snapped_user_location.into(),
                                current_step_linestring,
                            );

                            NavigationStateUpdate::Navigating {
                                snapped_user_location,
                                remaining_waypoints,
                                current_step: step,
                                distance_to_next_maneuver,
                            }
                        } else {
                            *guard = TripState::Complete;

                            NavigationStateUpdate::Arrived
                        }
                    }
                    // It's tempting to throw an error here, since the caller should know better, but
                    // a mistake like this is technically harmless.
                    TripState::Complete => NavigationStateUpdate::Arrived,
                }
            }
            Err(_) => {
                // The only way the mutex can become poisoned is if another caller panicked while
                // holding the mutex. In which case, there is no point in continuing.
                unreachable!("Poisoned mutex. This should never happen.");
            }
        }
    }
}
