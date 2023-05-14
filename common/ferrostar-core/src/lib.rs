pub mod navigation_controller;
pub mod routing_adapters;

use crate::routing_adapters::osrm::OsrmResponseParser;
use crate::routing_adapters::valhalla::ValhallaHttpRequestGenerator;
use geo_types::Coord;
pub use navigation_controller::NavigationController;
pub use routing_adapters::{
    error::{RoutingRequestGenerationError, RoutingResponseParseError},
    RouteAdapter, RouteRequest, RouteRequestGenerator, RouteResponseParser,
};

/// TODO: Docstring
///
/// NOTE: This type is unstable and is still under active development and should be
/// considered unstable.
#[derive(Debug)]
pub struct Route {
    geometry: Vec<GeographicCoordinate>,
}

#[derive(Clone, Copy, PartialEq, PartialOrd, Debug)]
pub struct GeographicCoordinate {
    lng: f64,
    lat: f64,
}

impl From<Coord> for GeographicCoordinate {
    fn from(value: Coord) -> Self {
        Self {
            lng: value.x,
            lat: value.y,
        }
    }
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
