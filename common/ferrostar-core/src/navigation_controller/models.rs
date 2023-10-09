use crate::{
    GeographicCoordinates, Route, RouteStep, SpokenInstruction, UserLocation, VisualInstructions,
};
use geo::LineString;

/// Internal state of the navigation controller.
pub(super) enum TripState {
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
        /// The ordered list of steps that remain in the trip.
        /// The step at the front of the list is always the current step.
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
        /// The current/active maneuver. Properties such as the distance will be updated live.
        current_step: RouteStep,
        visual_instructions: Option<VisualInstructions>,
        spoken_instruction: Option<SpokenInstruction>,
        // TODO: Communicate off-route and other state info
    },
    Arrived {
        visual_instructions: Option<VisualInstructions>,
        spoken_instruction: Option<SpokenInstruction>,
    },
}

#[derive(Debug, Copy, Clone)]
pub enum StepAdvanceMode {
    Manual,
    DistanceToLastWaypoint {
        /// Distance to the last waypoint, measured in meters, at which to advance to the next step
        distance: u16,
        /// The minimum required horizontal accuracy of the user location.
        /// Values larger than this will be ignored.
        minimum_horizontal_accuracy: u16,
    },
}

#[derive(Debug, Copy, Clone)]
pub struct NavigationControllerConfig {
    pub step_advance: StepAdvanceMode,
}
