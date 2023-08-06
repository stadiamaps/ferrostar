use crate::utils::snap_to_line;
use crate::{GeographicCoordinates, Route, RouteStep, SpokenInstruction, UserLocation};
use geo::{Coord, LineString};
use std::sync::Mutex;

/// Internal state of the navigation controller.
enum TripState {
    Navigating {
        last_user_location: UserLocation,
        snapped_user_location: UserLocation,
        route: Route,
        /// LineString (derived from route geometry) used for calculations like snapping.
        route_line_string: LineString,
        /// The ordered list of waypoints remaining to visit on this trip. Intermediate waypoints on
        /// the route to the final destination are discarded as they are visited.
        /// TODO: Do these need additional details like a name/label?
        remaining_waypoints: Vec<GeographicCoordinates>,
        remaining_steps: Vec<RouteStep>,
    },
    Complete,
}

/// Public updates pushed up to the direct user of the NavigationController.
pub enum NavigationStateUpdate {
    Navigating {
        snapped_user_location: UserLocation,
        /// The ordered list of waypoints remaining to visit on this trip. Intermediate waypoints on
        /// the route to the final destination are discarded as they are visited.
        remaining_waypoints: Vec<GeographicCoordinates>,
        /// The ordered list of steps to complete during the rest of the trip. Steps are discarded
        /// as they are completed.
        remaining_steps: Vec<RouteStep>,
        spoken_instruction: Option<SpokenInstruction>,
        // TODO: Banners
        // TODO: Communicate off-route and other state info
    },
    Arrived {
        spoken_instruction: Option<SpokenInstruction>,
    },
}

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
pub struct NavigationController {
    /// The last known location of the user. For all intents and purposes, the "user" is assumed
    /// to be at the location reported by their device (phone, car, etc.)
    ///
    /// NOTE: [Mutex] is used because UniFFI doesn't handle mutating struct operations
    /// very well. Others like [core::cell::RefCell] are not enough as the entire object is required to be both
    /// [Send] and [Sync], and [core::cell::RefCell] is explicitly `!Sync`.
    state: Mutex<TripState>,
}

impl NavigationController {
    /// Creates a new trip navigation controller given the user's last known location and a route.
    pub fn new(last_user_location: UserLocation, route: Route) -> Self {
        let remaining_waypoints = route.waypoints.clone();
        let remaining_steps = route.steps.clone();
        let route_line_string = route
            .geometry
            .iter()
            .map(|c| Coord { x: c.lng, y: c.lat })
            .collect();

        Self {
            state: Mutex::new(TripState::Navigating {
                last_user_location,
                snapped_user_location: snap_to_line(last_user_location, &route_line_string),
                route,
                route_line_string,
                remaining_waypoints,
                remaining_steps,
            }),
        }
    }

    pub fn update_user_location(&self, location: UserLocation) -> NavigationStateUpdate {
        match self.state.lock() {
            Ok(mut guard) => {
                match *guard {
                    TripState::Navigating {
                        mut last_user_location,
                        mut snapped_user_location,
                        ref route,
                        ref route_line_string,
                        ref remaining_waypoints,
                        ref remaining_steps,
                        ..
                    } => {
                        last_user_location = location;

                        //
                        // Navigation logic (rough draft)
                        //

                        // Find the nearest point on the route line
                        snapped_user_location = snap_to_line(location, &route_line_string);

                        // TODO: Check if the user's distance is > some threshold, possibly accounting for GPS error, mode of travel, etc.
                        // TODO: If so, flag that the user is off route so higher levels can recalculate if desired

                        // TODO: If on track, update the set of remaining waypoints and steps (drop from the list).
                        // IIUC these should always appear within the route itself, which simplifies the logic a bit.
                        // TBD: Do we want to support disjoint routes?

                        if remaining_waypoints.is_empty() {
                            *guard = TripState::Complete;

                            // TODO: Better info
                            NavigationStateUpdate::Arrived {
                                spoken_instruction: None,
                            }
                        } else {
                            // TODO: Maneuver instructions, banners, etc.
                            NavigationStateUpdate::Navigating {
                                snapped_user_location,
                                remaining_waypoints: remaining_waypoints.clone(),
                                remaining_steps: remaining_steps.clone(),
                                spoken_instruction: None,
                            }
                        }
                    }
                    // It's tempting to throw an error here, since the caller should know better, but
                    // a mistake like this is technically harmless.
                    TripState::Complete => NavigationStateUpdate::Arrived {
                        spoken_instruction: None,
                    },
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
