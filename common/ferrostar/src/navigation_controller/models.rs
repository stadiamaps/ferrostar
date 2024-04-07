use crate::deviation_detection::{RouteDeviation, RouteDeviationTracking};
use crate::models::{RouteStep, UserLocation, VisualInstruction, Waypoint};
use geo::LineString;

/// Internal state of the navigation controller.
#[derive(Debug, Clone, PartialEq, uniffi::Enum)]
pub enum TripState {
    Navigating {
        snapped_user_location: UserLocation,
        /// The ordered list of steps that remain in the trip.
        ///
        /// The step at the front of the list is always the current step.
        /// We currently assume that you cannot move backward to a previous step.
        remaining_steps: Vec<RouteStep>,
        /// Remaining waypoints to visit on the route.
        ///
        /// The waypoint at the front of the list is always the *next* waypoint "goal."
        /// Unlike the current step, there is no value in tracking the "current" waypoint,
        /// as the main use of waypoints is recalculation when the user deviates from the route.
        /// (In most use cases, a route will have only two waypoints, but more complex use cases
        /// may have multiple intervening points that are visited along the route.)
        /// This list is updated as the user advances through the route.
        remaining_waypoints: Vec<Waypoint>,
        /// The distance to the next maneuver, in meters.
        distance_to_next_maneuver: f64,
        /// The route deviation status: is the user following the route or not?
        deviation: RouteDeviation,
        /// The visual instruction that should be displayed to the user.
        visual_instruction: Option<VisualInstruction>,
        // TODO: Do current visual instruction and spoken instruction belong here?
    },
    Complete,
}

pub enum StepAdvanceStatus {
    /// Navigation has advanced, and the information on the next step is embedded.
    Advanced {
        step: RouteStep,
        linestring: LineString,
    },
    /// Navigation has reached the end of the route.
    EndOfRoute,
}

#[derive(Debug, Copy, Clone, uniffi::Enum)]
pub enum StepAdvanceMode {
    /// Never advances to the next step automatically
    Manual,
    /// Automatically advances when the user's location is close enough to the end of the step
    DistanceToEndOfStep {
        /// Distance to the last waypoint in the step, measured in meters, at which to advance.
        distance: u16,
        /// The minimum required horizontal accuracy of the user location, in meters.
        /// Values larger than this cannot trigger a step advance.
        minimum_horizontal_accuracy: u16,
    },
    /// Automatically advances when the user's distance to the *next* step's linestring  is less
    /// than the distance to the current step's linestring.
    RelativeLineStringDistance {
        /// The minimum required horizontal accuracy of the user location, in meters.
        /// Values larger than this cannot trigger a step advance.
        minimum_horizontal_accuracy: u16,
        /// At this (optional) distance, navigation should advance to the next step regardless
        /// of which LineString appears closer.
        automatic_advance_distance: Option<u16>,
    },
}

#[derive(Clone, uniffi::Record)]
pub struct NavigationControllerConfig {
    pub step_advance: StepAdvanceMode,
    pub route_deviation_tracking: RouteDeviationTracking,
}
