pub mod navigation_controller;
pub mod routing_adapters;
pub(crate) mod utils;

use crate::routing_adapters::osrm::OsrmResponseParser;
use crate::routing_adapters::valhalla::ValhallaHttpRequestGenerator;
use crate::RoutingResponseParseError::ParseError;
use geo::Coord;
pub use navigation_controller::{NavigationController, NavigationStateUpdate};
use polyline::decode_polyline;
pub use routing_adapters::{
    error::{RoutingRequestGenerationError, RoutingResponseParseError},
    RouteAdapter, RouteRequest, RouteRequestGenerator, RouteResponseParser,
};
use std::time::SystemTime;

#[derive(Clone, Copy, PartialEq, PartialOrd, Debug)]
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
/// stricter is needed.
#[derive(Clone, Copy, Debug)]
pub struct RouteStep {
    /// The starting location of the step (start of the maneuver).
    pub start_location: GeographicCoordinates,
    // TODO: Do we need to also include the end location?
    /// The distance, in meters, to travel along the route after the maneuver to reach the next step.
    pub distance: f64,
    // TODO: Maneuver details
}

impl RouteStep {
    fn from_osrm(
        value: &routing_adapters::osrm::models::RouteStep,
        polyline_precision: u32,
    ) -> Result<Self, RoutingResponseParseError> {
        let start_location = decode_polyline(&value.geometry, polyline_precision)
            .map_err(|error| RoutingResponseParseError::ParseError { error })?
            .coords()
            .map(|coord| GeographicCoordinates::from(*coord))
            .take(1)
            .next()
            .ok_or(ParseError {
                error: "No coordinates in geometry".to_string(),
            })?;
        Ok(RouteStep {
            start_location,
            distance: value.distance,
        })
    }
}

pub struct SpokenInstruction {
    /// Plain-text instruction which can be synthesized with a TTS engine.
    pub text: String,
    /// Speech Synthesis Markup Language, which should be preferred by clients capable of understanding it.
    pub ssml: Option<String>,
}

//
// Helpers that are only exposed via the FFI interface.
//
// Most of these exist for convenience since the UDL understandably isn't implementing a
// full Rust type system and it would be a bunch of boilerplate to expose the foll objects.
// Instead we use top-level functions to return dynamic objects conforming to the trait.
//

fn create_valhalla_request_generator(
    endpoint_url: String,
    profile: String,
) -> Box<dyn RouteRequestGenerator> {
    Box::new(ValhallaHttpRequestGenerator::new(endpoint_url, profile))
}

fn create_osrm_response_parser(polyline_precision: u32) -> Box<dyn RouteResponseParser> {
    Box::new(OsrmResponseParser::new(polyline_precision))
}

uniffi::include_scaffolding!("ferrostar");
