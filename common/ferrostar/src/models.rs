//! Common data models.
//!
//! Quick tour:
//! - [`Route`]: Common notion of what a route is; You can go top-down from here if you're curious.
//! - [`Waypoint`]: Points that a user is intending to traverse; interesting because there are multiple kinds of them.
//! - [`SpokenInstruction`] and [`VisualInstruction`]: Audiovisual prompts as the user progresses through the route.
//! - [`GeographicCoordinate`] and [`BoundingBox`]: Geographic primitives
//!   (providing a shared language and type definition across multiple platforms).

#[cfg(feature = "alloc")]
use alloc::{string::String, vec::Vec};
use geo::{Coord, LineString, Point, Rect};
#[cfg(feature = "uniffi")]
use polyline::encode_coordinates;
use serde::{Deserialize, Serialize};
use serde_json::Value;

#[cfg(all(feature = "std", not(feature = "web-time")))]
use std::time::SystemTime;

#[cfg(feature = "web-time")]
use web_time::SystemTime;

#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

use chrono::{DateTime, Utc};
use std::collections::HashMap;
use uuid::Uuid;

use crate::algorithms::get_linestring;

#[derive(Debug)]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
pub enum ModelError {
    #[cfg_attr(
        feature = "std",
        error("Failed to generate a polyline from route coordinates: {error}.")
    )]
    PolylineGenerationError { error: String },
}

/// A geographic coordinate in WGS84.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct GeographicCoordinate {
    /// The latitude (in degrees).
    pub lat: f64,
    /// The Longitude (in degrees).
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
/// Within the context of Ferrostar, a route request consists of exactly one [`UserLocation`]
/// and at least one [`Waypoint`]. The route starts from the user's location (which may
/// contain other useful information like their current course for the [`crate::routing_adapters::RouteRequestGenerator`]
/// to use) and proceeds through one or more waypoints.
///
/// Waypoints are used during route calculation, are tracked throughout the lifecycle of a trip,
/// and are used for recalculating when the user deviates from the expected route.
///
/// Note that support for properties beyond basic geographic coordinates varies by routing engine.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct Waypoint {
    pub coordinate: GeographicCoordinate,
    pub kind: WaypointKind,
}

/// Describes characteristics of the waypoint for the routing backend.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub enum WaypointKind {
    /// Starts or ends a leg of the trip.
    ///
    /// Most routing engines will generate arrival and departure instructions.
    Break,
    /// A waypoint that is simply passed through, but will not have any arrival or departure instructions.
    Via,
}

/// A geographic bounding box defined by its corners.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct BoundingBox {
    /// The southwest corner of the bounding box.
    pub sw: GeographicCoordinate,
    /// The northeast corner of the bounding box.
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
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct Heading {
    /// The heading in degrees relative to true north.
    pub true_heading: u16,
    /// The platform specific accuracy of the heading value.
    pub accuracy: u16,
    /// The time at which the heading was recorded.
    pub timestamp: SystemTime,
}

/// The direction in which the user/device is observed to be traveling.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct CourseOverGround {
    /// The direction in which the user's device is traveling, measured in clockwise degrees from
    /// true north (N = 0, E = 90, S = 180, W = 270).
    pub degrees: u16,
    /// The accuracy of the course value, measured in degrees.
    pub accuracy: Option<u16>,
}

impl CourseOverGround {
    /// # Arguments
    ///
    /// - degrees: The direction in which the user's device is traveling, measured in clockwise degrees from
    ///            true north (N = 0, E = 90, S = 180, W = 270).
    ///            NOTE: Input values must lie in the range [0, 360).
    /// - accuracy: the accuracy of the course value, measured in degrees.
    pub fn new(degrees: f64, accuracy: Option<u16>) -> Self {
        debug_assert!(degrees >= 0.0 && degrees < 360.0);
        Self {
            degrees: degrees.round() as u16,
            accuracy,
        }
    }
}

/// The speed of the user from the location provider.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct Speed {
    /// The user's speed in meters per second.
    pub value: f64,
    /// The accuracy of the speed value, measured in meters per second.
    pub accuracy: Option<f64>,
}

#[cfg(feature = "wasm-bindgen")]
mod system_time_format {
    use serde::{self, Deserialize, Deserializer, Serializer};

    #[cfg(all(feature = "std", not(feature = "web-time")))]
    use std::time::{Duration, SystemTime, UNIX_EPOCH};

    #[cfg(feature = "web-time")]
    use web_time::{Duration, SystemTime, UNIX_EPOCH};

    pub fn serialize<S>(time: &SystemTime, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let duration = time
            .duration_since(UNIX_EPOCH)
            .map_err(serde::ser::Error::custom)?;
        let millis = duration.as_secs() * 1000 + duration.subsec_millis() as u64;
        serializer.serialize_u64(millis)
    }

    pub fn deserialize<'de, D>(deserializer: D) -> Result<SystemTime, D::Error>
    where
        D: Deserializer<'de>,
    {
        let millis = u64::deserialize(deserializer)?;
        Ok(UNIX_EPOCH + Duration::from_millis(millis))
    }
}

/// The location of the user that is navigating.
///
/// In addition to coordinates, this includes estimated accuracy and course information,
/// which can influence navigation logic and UI.
///
/// NOTE: Heading is absent on purpose.
/// Heading updates are not related to a change in the user's location.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct UserLocation {
    pub coordinates: GeographicCoordinate,
    /// The estimated accuracy of the coordinate (in meters)
    pub horizontal_accuracy: f64,
    pub course_over_ground: Option<CourseOverGround>,
    #[cfg_attr(test, serde(skip_serializing))]
    #[cfg_attr(feature = "wasm-bindgen", serde(with = "system_time_format"))]
    pub timestamp: SystemTime,
    pub speed: Option<Speed>,
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
#[derive(Clone, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
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
#[cfg(feature = "uniffi")]
#[uniffi::export]
fn get_route_polyline(route: &Route, precision: u32) -> Result<String, ModelError> {
    encode_coordinates(route.geometry.iter().map(|c| Coord::from(*c)), precision).map_err(|error| {
        ModelError::PolylineGenerationError {
            error: error.to_string(),
        }
    })
}

/// A maneuver (such as a turn or merge) followed by travel of a certain distance until reaching
/// the next step.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct RouteStep {
    /// The full route geometry for this step.
    pub geometry: Vec<GeographicCoordinate>,
    /// The distance, in meters, to travel along the route after the maneuver to reach the next step.
    pub distance: f64,
    /// The estimated duration, in seconds, that it will take to complete this step.
    pub duration: f64,
    /// The name of the road being traveled on (useful for certain UI styles).
    pub road_name: Option<String>,
    /// A list of exits (name or number).
    pub exits: Vec<String>,
    /// A description of the maneuver (ex: "Turn wright onto main street").
    ///
    /// Note for UI implementers: the context this appears in (or doesn't)
    /// depends somewhat on your use case and routing engine.
    /// For example, this field is useful as a written instruction in Valhalla.
    pub instruction: String,
    /// A list of instructions for visual display (usually as banners) at specific points along the step.
    pub visual_instructions: Vec<VisualInstruction>,
    /// A list of prompts to announce (via speech synthesis) at specific points along the step.
    pub spoken_instructions: Vec<SpokenInstruction>,
    /// A list of json encoded strings representing annotations between each coordinate along the step.
    pub annotations: Option<Vec<String>>,
    /// A list of incidents that occur along the step.
    pub incidents: Vec<Incident>,
}

impl RouteStep {
    pub(crate) fn get_linestring(&self) -> LineString {
        get_linestring(&self.geometry)
    }

    /// Gets the active visual instruction at a specific point along the step.
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

    /// Gets the spoken instruction at a specific point along the step.
    ///
    /// Note to platform implementers: some care is needed with this.
    /// Unlike visual instructions, which can be changed without much consequence,
    /// speech synthesis takes time to complete.
    /// Take care to characteristics of your synthesis engine,
    /// including whether utterances are queued or cut off the currently playing one.
    /// You will also need some sort of check to ensure you don't make the same announcement
    /// more times than necessary.
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

    /// Get the annotation data at a specific point along the step.
    ///
    /// `at_coordinate_index` is the index of the coordinate in the step geometry.
    pub fn get_annotation_at_current_index(&self, at_coordinate_index: u64) -> Option<String> {
        self.annotations
            .as_ref()
            .and_then(|annotations| annotations.get(at_coordinate_index as usize).cloned())
    }
}

/// An instruction that can be synthesized using a TTS engine to announce an upcoming maneuver.
///
/// Note that these do not have any locale information attached.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi))]
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
    #[cfg_attr(feature = "wasm-bindgen", tsify(type = "string"))]
    pub utterance_id: Uuid,
}

/// The broad class of maneuver to perform.
///
/// This is usually combined with [`ManeuverModifier`] in [`VisualInstructionContent`].
#[derive(Deserialize, Debug, Copy, Clone, Eq, PartialEq, Serialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
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

/// Additional information to further specify a [`ManeuverType`].
#[derive(Deserialize, Debug, Copy, Clone, Eq, PartialEq, Serialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
#[serde(rename_all = "lowercase")]
pub enum ManeuverModifier {
    #[serde(rename = "uturn")]
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

/// The type of incident that has occurred.
#[derive(Deserialize, Debug, Copy, Clone, Eq, PartialEq, Serialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
#[serde(rename_all = "snake_case")]
pub enum IncidentType {
    Accident,
    Congestion,
    Construction,
    DisabledVehicle,
    LaneRestriction,
    MassTransit,
    Miscellaneous,
    OtherNews,
    PlannedEvent,
    RoadClosure,
    RoadHazard,
    Weather,
}

/// The impact of the incident that has occurred.
#[derive(Deserialize, Debug, Copy, Clone, Eq, PartialEq, Serialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
#[serde(rename_all = "lowercase")]
pub enum Impact {
    Unknown,
    Critical,
    Major,
    Minor,
    Low,
}

/// The lane type blocked by the incident.
#[derive(Debug, Copy, Clone, Eq, PartialEq, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
#[serde(rename_all = "lowercase")]
pub enum BlockedLane {
    Left,
    #[serde(rename = "left center")]
    LeftCenter,
    #[serde(rename = "left turn lane")]
    LeftTurnLane,
    Center,
    Right,
    #[serde(rename = "right center")]
    RightCenter,
    #[serde(rename = "right turn lane")]
    RightTurnLane,
    #[serde(rename = "hov")]
    HOV,
}

/// Details about congestion for an incident.
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct Congestion {
    /// The level of congestion caused by the incident.
    ///
    /// 0 = no congestion
    ///
    /// 100 = road closed
    ///
    /// Other values mean no congestion was calculated
    pub value: u8,
}

/// An incident affecting the free flow of traffic,
/// such as constructions, accidents, and congestion.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct Incident {
    /// A unique identifier for the incident.
    pub id: String,
    /// The type of incident.
    pub incident_type: IncidentType,
    /// A short description of the incident.
    pub description: Option<String>,
    /// A longer description of the incident.
    pub long_description: Option<String>,
    /// The time at which the incident was *last* created.
    ///
    /// NB: This can change throughout the life of the incident.
    #[cfg_attr(feature = "wasm-bindgen", tsify(type = "Date | null"))]
    pub creation_time: Option<DateTime<Utc>>,
    /// The time at which the incident started or is expected to start (ex: planned closure).
    #[cfg_attr(feature = "wasm-bindgen", tsify(type = "Date | null"))]
    pub start_time: Option<DateTime<Utc>>,
    /// The time at which the incident ended or is expected to end.
    #[cfg_attr(feature = "wasm-bindgen", tsify(type = "Date | null"))]
    pub end_time: Option<DateTime<Utc>>,
    /// The level of impact to traffic.
    pub impact: Option<Impact>,
    /// Lanes which are blocked by the incident.
    pub lanes_blocked: Vec<BlockedLane>,
    /// Info about the amount of congestion on the road around the incident.
    pub congestion: Option<Congestion>,
    /// Is the road completely closed?
    pub closed: Option<bool>,
    /// The index into the [`RouteStep`] geometry where the incident starts.
    pub geometry_index_start: u64,
    /// The index into the [`RouteStep`] geometry where the incident ends.
    pub geometry_index_end: Option<u64>,
    /// Optional additional information about the type of incident (free-form text).
    pub sub_type: Option<String>,
    /// Optional descriptions about the type of incident (free-form text).
    pub sub_type_description: Option<String>,
    /// The ISO 3166-1 alpha-2 code of the country in which the incident occurs.
    pub iso_3166_1_alpha2: Option<String>,
    /// The ISO 3166-1 alpha-3 code of the country in which the incident occurs.
    pub iso_3166_1_alpha3: Option<String>,
    /// A list of road names affected by the incident.
    pub affected_road_names: Vec<String>,
    /// The bounding box over which the incident occurs.
    pub bbox: Option<BoundingBox>,
}

/// The content of a visual instruction.
#[derive(Debug, Clone, Eq, PartialEq, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct LaneInfo {
    pub active: bool,
    pub directions: Vec<String>,
    pub active_direction: Option<String>,
}

/// The content of a visual instruction.
#[derive(Debug, Clone, Eq, PartialEq, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct VisualInstructionContent {
    /// The text to display.
    pub text: String,
    /// A standardized maneuver type (if any).
    pub maneuver_type: Option<ManeuverType>,
    /// A standardized maneuver modifier (if any).
    pub maneuver_modifier: Option<ManeuverModifier>,
    /// If applicable, the number of degrees you need to go around the roundabout before exiting.
    ///
    /// For example, entering and exiting the roundabout in the same direction of travel
    /// (as if you had gone straight, apart from the detour)
    /// would be an exit angle of 180 degrees.
    pub roundabout_exit_degrees: Option<u16>,
    /// Detailed information about the lanes. This is typically only present in sub-maneuver instructions.
    pub lane_info: Option<Vec<LaneInfo>>,
    /// The exit number (or similar identifier like "8B").
    pub exit_numbers: Vec<String>,
}

/// An instruction for visual display (usually as banners) at a specific point along a [`RouteStep`].
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct VisualInstruction {
    /// The primary instruction content.
    ///
    /// This is usually given more visual weight.
    pub primary_content: VisualInstructionContent,
    /// Optional secondary instruction content.
    pub secondary_content: Option<VisualInstructionContent>,
    /// Optional sub-maneuver instruction content.
    pub sub_content: Option<VisualInstructionContent>,
    /// How far (in meters) from the upcoming maneuver the instruction should start being displayed
    pub trigger_distance_before_maneuver: f64,
}

/// A flat annotations string value map that can be used to store arbitrary
/// annotation values.
#[derive(Deserialize, Serialize, Debug, Clone, PartialEq)]
pub struct AnyAnnotationValue {
    #[serde(flatten)]
    pub value: HashMap<String, Value>,
}

#[cfg(test)]
#[cfg(feature = "uniffi")]
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
