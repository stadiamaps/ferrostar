use geo::{Coord, Point};
use serde::{Deserialize};
use std::time::SystemTime;

#[cfg(test)]
use serde::Serialize;

#[derive(Clone, Copy, PartialEq, PartialOrd, Debug)]
#[cfg_attr(test, derive(Serialize))]
pub struct GeographicCoordinates {
    pub lng: f64,
    pub lat: f64,
}

impl From<Coord> for GeographicCoordinates {
    fn from(value: Coord) -> Self {
        Self {
            lng: value.x,
            lat: value.y,
        }
    }
}

impl From<GeographicCoordinates> for Coord {
    fn from(value: GeographicCoordinates) -> Self {
        Self {
            x: value.lng,
            y: value.lat,
        }
    }
}

impl From<GeographicCoordinates> for Point {
    fn from(value: GeographicCoordinates) -> Self {
        Self { 0: value.into() }
    }
}

/// The direction in which the user/device is observed to be traveling.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug)]
pub struct CourseOverGround {
    /// The direction in which the user's device is traveling, measured in clockwise degrees from
    /// true north (N = 0, E = 90, S = 180, W = 270).
    pub degrees: u16,
    /// The accuracy of the course value, measured in degrees.
    pub accuracy: u16,
}

impl CourseOverGround {
    pub fn new(degrees: u16, accuracy: u16) -> Self {
        Self { degrees, accuracy }
    }
}

/// The location of the user that is navigating.
///
/// In addition to coordinates, this includes estimated accuracy and course information,
/// which can influence navigation logic and UI.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug)]
pub struct UserLocation {
    pub coordinates: GeographicCoordinates,
    /// The estimated accuracy of the coordinate (in meters)
    pub horizontal_accuracy: f64,
    pub course_over_ground: Option<CourseOverGround>,
    // TODO: Decide if we want to include heading in the user location, if/how we should factor it in, and how to handle it on Android
    pub timestamp: SystemTime,
}

/// Information describing the series of steps needed to travel between two or more points.
///
/// NOTE: This type is unstable and is still under active development and should be
/// considered unstable.
#[derive(Debug)]
#[cfg_attr(test, derive(Serialize))]
pub struct Route {
    pub geometry: Vec<GeographicCoordinates>,
    /// The ordered list of waypoints to visit, including the starting point.
    /// Note that this is distinct from the *geometry* which includes all points visited.
    /// A waypoint represents a start/end point for a route leg.
    pub waypoints: Vec<GeographicCoordinates>,
    pub steps: Vec<RouteStep>,
}

/// A maneuver (such as a turn or merge) followed by travel of a certain distance until reaching
/// the next step.
///
/// NOTE: OSRM specifies this rather precisely as "travel along a single way to the subsequent step"
/// but we will intentionally define this somewhat looser unless/until it becomes clear something
///
#[derive(Clone, Debug, PartialEq)]
#[cfg_attr(test, derive(Serialize))]
pub struct RouteStep {
    /// The starting location of the step (start of the maneuver).
    pub start_location: GeographicCoordinates,
    /// The ending location of the step (end of the maneuver).
    pub end_location: GeographicCoordinates,
    /// The distance, in meters, to travel along the route after the maneuver to reach the next step.
    pub distance: f64,
    pub road_name: Option<String>,
    pub instruction: String,
    pub visual_instructions: Vec<VisualInstructions>,
}

// TODO: trigger_at doesn't really have to live in the public interface; figure out if we want to have a separate FFI vs internal type

#[derive(Debug, PartialEq)]
pub struct SpokenInstruction {
    /// Plain-text instruction which can be synthesized with a TTS engine.
    pub text: String,
    /// Speech Synthesis Markup Language, which should be preferred by clients capable of understanding it.
    pub ssml: Option<String>,
    pub trigger_at: GeographicCoordinates,
}

#[derive(Debug, Clone, PartialEq)]
#[cfg_attr(test, derive(Serialize))]
pub struct VisualInstructions {
    pub primary_content: VisualInstructionContent,
    pub secondary_content: Option<VisualInstructionContent>,
    pub trigger_at: GeographicCoordinates,
}

/// Indicates the type of maneuver to perform.
///
/// Frequently used in conjunction with [ManeuverModifier].
#[derive(Deserialize, Debug, Copy, Clone, Eq, PartialEq)]
#[cfg_attr(test, derive(Serialize))]
#[serde(rename_all = "lowercase")]
pub enum ManeuverType {
    Turn,
    #[serde(rename = "new name")]
    NewName,
    Depart,
    Arrive,
    Merge,
    #[serde(rename = "on ramp")]
    OnRamp,
    #[serde(rename = "off ramp")]
    OffRamp,
    Fork,
    #[serde(rename = "end of road")]
    EndOfRoad,
    Continue,
    Roundabout,
    Rotary,
    #[serde(rename = "roundabout turn")]
    RoundaboutTurn,
    Notification,
    #[serde(rename = "exit roundabout")]
    ExitRoundabout,
    #[serde(rename = "exit rotary")]
    ExitRotary,
}

/// Specifies additional information about a [ManeuverType]
#[derive(Deserialize, Debug, Copy, Clone, Eq, PartialEq)]
#[cfg_attr(test, derive(Serialize))]
#[serde(rename_all = "lowercase")]
pub enum ManeuverModifier {
    UTurn,
    #[serde(rename = "sharp right")]
    SharpRight,
    Right,
    #[serde(rename = "slight right")]
    SlightRight,
    Straight,
    #[serde(rename = "slight left")]
    SlightLeft,
    Left,
    #[serde(rename = "sharp left")]
    SharpLeft,
}

#[derive(Debug, Clone, Eq, PartialEq)]
#[cfg_attr(test, derive(Serialize))]
pub struct VisualInstructionContent {
    pub text: String,
    pub maneuver_type: Option<ManeuverType>,
    pub maneuver_modifier: Option<ManeuverModifier>,
    pub degrees: Option<i16>,
}
