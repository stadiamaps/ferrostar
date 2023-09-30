use geo::LineString;
use crate::{GeographicCoordinates, Route, RouteStep, SpokenInstruction, UserLocation, VisualInstructions};

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