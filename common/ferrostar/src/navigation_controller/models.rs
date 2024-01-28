use crate::models::{RouteStep, UserLocation};
use geo::LineString;

/// Internal state of the navigation controller.
#[derive(Debug, Clone, PartialEq, uniffi::Enum)]
pub enum TripState {
    Navigating {
        snapped_user_location: UserLocation,
        /// The ordered list of steps that remain in the trip.
        /// The step at the front of the list is always the current step.
        /// We currently assume that you cannot move backward to a previous step.
        remaining_steps: Vec<RouteStep>,
        /// The distance to the next maneuver, in meters.
        distance_to_next_maneuver: f64,
        /// The deviation of the user from the expected route line.
        deviation_from_route_line: Option<f64>,
    },
    Complete,
}

pub enum StepAdvanceStatus {
    Advanced {
        step: RouteStep,
        linestring: LineString,
    },
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
        /// The minimum required horizontal accuracy of the user location.
        /// Values larger than this cannot trigger a step advance.
        minimum_horizontal_accuracy: u16,
    },
    /// Automatically advances when the user's distance to the *next* step's linestring  is less
    /// than the distance to the current step's linestring.
    RelativeLineStringDistance {
        /// The minimum required horizontal accuracy of the user location.
        /// Values larger than this cannot trigger a step advance.
        minimum_horizontal_accuracy: u16,
        /// At this (optional) distance, navigation should advance to the next step regardless
        /// of which LineString appears closer.
        automatic_advance_distance: Option<u16>,
    },
}

#[derive(Debug, Copy, Clone, uniffi::Record)]
pub struct NavigationControllerConfig {
    pub step_advance: StepAdvanceMode,
}
