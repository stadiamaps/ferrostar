use geo::{Coord, LineString, Point, Rect};
use polyline::encode_coordinates;
use serde::Deserialize;
use std::time::SystemTime;

#[cfg(test)]
use serde::Serialize;
use uuid::Uuid;

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ModelError {
    #[error("Failed to generate a polyline from route coordinates: {error}.")]
    PolylineGenerationError { error: String },
}

/// A geographic coordinate in WGS84.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct GeographicCoordinate {
    pub lat: f64,
    pub lng: f64,
}

impl From<Coord> for GeographicCoordinate {
    fn from(value: Coord) -> Self {
        Self {
            lat: value.y,
            lng: value.x,
        }
    }
}

impl From<Point> for GeographicCoordinate {
    fn from(value: Point) -> Self {
        Self {
            lat: value.y(),
            lng: value.x(),
        }
    }
}

impl From<GeographicCoordinate> for Coord {
    fn from(value: GeographicCoordinate) -> Self {
        Self {
            x: value.lng,
            y: value.lat,
        }
    }
}

impl From<GeographicCoordinate> for Point {
    fn from(value: GeographicCoordinate) -> Self {
        Self(value.into())
    }
}

/// A waypoint along a route.
///
/// Within the context of Ferrostar, a route request consists of exactly one [UserLocation]
/// and at least one [Waypoint]. The route starts from the user's location (which may
/// contain other useful information like their current course for the [crate::routing_adapters::RouteRequestGenerator]
/// to use) and proceeds through one or more waypoints.
///
/// Waypoints are used during route calculation, are tracked throughout the lifecycle of a trip,
/// and are used for recalculating when the user deviates from the expected route.
///
/// Note that support for properties beyond basic geographic coordinates varies by routing engine.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct Waypoint {
    pub coordinate: GeographicCoordinate,
    pub kind: WaypointKind,
}

/// Describes characteristics of the waypoint for the routing backend.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, uniffi::Enum)]
#[cfg_attr(test, derive(Serialize))]
pub enum WaypointKind {
    /// Starts or ends a leg of the trip.
    ///
    /// Most routing engines will generate arrival and departure instructions.
    Break,
    /// A waypoint that is simply passed through, but will not have any arrival or departure instructions.
    Via,
}

#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct BoundingBox {
    pub sw: GeographicCoordinate,
    pub ne: GeographicCoordinate,
}

impl From<Rect> for BoundingBox {
    fn from(value: Rect) -> Self {
        Self {
            sw: value.min().into(),
            ne: value.max().into(),
        }
    }
}

/// The heading of the user/device.
///
/// Ferrostar prefers course over ground, but may use heading in some cases.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, uniffi::Record)]
pub struct Heading {
    /// The heading in degrees relative to true north.
    pub true_heading: u16,
    /// The platform specific accuracy of the heading value.
    pub accuracy: u16,
    /// The time at which the heading was recorded.
    pub timestamp: SystemTime,
}

/// The direction in which the user/device is observed to be traveling.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
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
///
/// NOTE: Heading is absent on purpose.
/// Heading updates are not related to a change in the user's location.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct UserLocation {
    pub coordinates: GeographicCoordinate,
    /// The estimated accuracy of the coordinate (in meters)
    pub horizontal_accuracy: f64,
    pub course_over_ground: Option<CourseOverGround>,
    #[cfg_attr(test, serde(skip_serializing))]
    pub timestamp: SystemTime,
    pub speed: Option<f64>,
    pub speed_accuracy: Option<f64>
}

impl From<UserLocation> for Point {
    fn from(val: UserLocation) -> Point {
        Point::new(val.coordinates.lng, val.coordinates.lat)
    }
}

/// Information describing the series of steps needed to travel between two or more points.
///
/// NOTE: This type is unstable and is still under active development and should be
/// considered unstable.
#[derive(Clone, Debug, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct Route {
    pub geometry: Vec<GeographicCoordinate>,
    pub bbox: BoundingBox,
    /// The total route distance, in meters.
    pub distance: f64,
    /// The ordered list of waypoints to visit, including the starting point.
    /// Note that this is distinct from the *geometry* which includes all points visited.
    /// A waypoint represents a start/end point for a route leg.
    pub waypoints: Vec<Waypoint>,
    pub steps: Vec<RouteStep>,
}

/// Helper function for getting the route as an encoded polyline.
///
/// Mostly used for debugging.
#[uniffi::export]
fn get_route_polyline(route: &Route, precision: u32) -> Result<String, ModelError> {
    encode_coordinates(route.geometry.iter().map(|c| Coord::from(*c)), precision)
        .map_err(|error| ModelError::PolylineGenerationError { error })
}

/// A maneuver (such as a turn or merge) followed by travel of a certain distance until reaching
/// the next step.
///
/// NOTE: OSRM specifies this rather precisely as "travel along a single way to the subsequent step"
/// but we will intentionally define this somewhat looser unless/until it becomes clear something
///
#[derive(Clone, Debug, PartialEq, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct RouteStep {
    pub geometry: Vec<GeographicCoordinate>,
    /// The distance, in meters, to travel along the route after the maneuver to reach the next step.
    pub distance: f64,
    /// The duration, in seconds the router estimates it will take to travel the step.
    pub duration: f64,
    pub road_name: Option<String>,
    pub instruction: String,
    pub visual_instructions: Vec<VisualInstruction>,
    pub spoken_instructions: Vec<SpokenInstruction>,
}

impl RouteStep {
    // TODO: Memoize or something later
    pub(crate) fn get_linestring(&self) -> LineString {
        LineString::from_iter(self.geometry.iter().map(|coord| Coord {
            x: coord.lng,
            y: coord.lat,
        }))
    }

    /// Gets the active visual instruction given the user's progress along the step.
    pub fn get_active_visual_instruction(
        &self,
        distance_to_end_of_step: f64,
    ) -> Option<&VisualInstruction> {
        // Plain English: finds the *last* instruction where we are past the trigger distance.
        //
        // We have a fudge factor to account for imprecision in calculation methodologies from different engines and CPUs,
        // particularly at the start of a step.
        self.visual_instructions.iter().rev().find(|instruction| {
            distance_to_end_of_step - instruction.trigger_distance_before_maneuver <= 5.0
        })
    }

    /// Gets the current (latest?) spoken instruction given the user's progress along the step.
    pub fn get_current_spoken_instruction(
        &self,
        distance_to_end_of_step: f64,
    ) -> Option<&SpokenInstruction> {
        // Plain English: finds the *last* instruction where we are past the trigger distance.
        //
        // We have a fudge factor to account for imprecision in calculation methodologies from different engines and CPUs,
        // particularly at the start of a step.
        self.spoken_instructions.iter().rev().find(|instruction| {
            distance_to_end_of_step - instruction.trigger_distance_before_maneuver <= 5.0
        })
    }
}

/// An instruction that can be synthesized using a TTS engine to announce an upcoming maneuver.
///
/// Note that these do not have any locale information attached.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct SpokenInstruction {
    /// Plain-text instruction which can be synthesized with a TTS engine.
    pub text: String,
    /// Speech Synthesis Markup Language, which should be preferred by clients capable of understanding it.
    pub ssml: Option<String>,
    /// How far (in meters) from the upcoming maneuver the instruction should start being displayed
    pub trigger_distance_before_maneuver: f64,
    /// A unique identifier for this instruction.
    ///
    /// This is provided so that platform-layer integrations can easily disambiguate between distinct utterances,
    /// which may have the same textual content.
    /// UUIDs conveniently fill this purpose.
    ///
    /// NOTE: While it is possible to deterministically create UUIDs, we do not do so at this time.
    /// This should be theoretically possible though if someone cares to write up a proposal and a PR.
    #[cfg_attr(test, serde(skip_serializing))]
    pub utterance_id: Uuid,
}

/// Indicates the type of maneuver to perform.
///
/// Frequently used in conjunction with [ManeuverModifier].
#[derive(Deserialize, Debug, Copy, Clone, Eq, PartialEq, uniffi::Enum)]
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
#[derive(Deserialize, Debug, Copy, Clone, Eq, PartialEq, uniffi::Enum)]
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

#[derive(Debug, Clone, Eq, PartialEq, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct VisualInstructionContent {
    pub text: String,
    pub maneuver_type: Option<ManeuverType>,
    pub maneuver_modifier: Option<ManeuverModifier>,
    pub roundabout_exit_degrees: Option<u16>,
}

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
#[cfg_attr(test, derive(Serialize))]
pub struct VisualInstruction {
    pub primary_content: VisualInstructionContent,
    pub secondary_content: Option<VisualInstructionContent>,
    /// How far (in meters) from the upcoming maneuver the instruction should start being displayed
    pub trigger_distance_before_maneuver: f64,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_polyline_encode() {
        let sw = GeographicCoordinate { lng: 0.0, lat: 0.0 };
        let ne = GeographicCoordinate { lng: 1.0, lat: 1.0 };
        let route = Route {
            geometry: vec![sw, ne],
            bbox: BoundingBox { sw, ne },
            distance: 0.0,
            waypoints: vec![],
            steps: vec![],
        };

        let polyline5 = get_route_polyline(&route, 5).expect("Unable to encode polyline for route");
        insta::assert_yaml_snapshot!(polyline5);

        let polyline6 = get_route_polyline(&route, 6).expect("Unable to encode polyline for route");
        insta::assert_yaml_snapshot!(polyline6);
    }
}
