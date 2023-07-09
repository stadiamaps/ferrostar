pub mod navigation_controller;
pub mod routing_adapters;
pub(crate) mod utils;

use crate::routing_adapters::osrm::OsrmResponseParser;
use crate::routing_adapters::valhalla::ValhallaHttpRequestGenerator;
use geo_types::Coord;
pub use navigation_controller::{NavigationController, NavigationStateUpdate};
pub use routing_adapters::{
    error::{RoutingRequestGenerationError, RoutingResponseParseError},
    RouteAdapter, RouteRequest, RouteRequestGenerator, RouteResponseParser,
};

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

/// The direction in which the user/device is traveling.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug)]
pub struct Course {
    /// The direction in which the user's device is traveling, measured in clockwise degrees from
    /// true north (N = 0, E = 90, S = 180, W = 270).
    pub degrees: u16,
    /// The accuracy of the course value, measured in degrees.
    pub accuracy: u16,
}

impl Course {
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
    pub course: Option<Course>,
}

/// Information describing the series of maneuvers to travel between two or more points.
///
/// NOTE: This type is unstable and is still under active development and should be
/// considered unstable.
#[derive(Debug)]
pub struct Route {
    pub geometry: Vec<GeographicCoordinates>,
    /// The ordered list of waypoints to visit, including the starting point.
    pub waypoints: Vec<GeographicCoordinates>,
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
