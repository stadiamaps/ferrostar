//! # Ferrostar
//!
//! Ferrostar is a modern SDK for building turn-by-turn navigation applications.
//!
//! See the [README on GitHub](https://github.com/stadiamaps/ferrostar) for the moment,
//! as this is still under extremely active development,
//! and proper documentation is still in flux.

pub mod algorithms;
pub mod deviation_detection;
pub mod models;
pub mod navigation_controller;
pub mod routing_adapters;
pub mod simulation;

use crate::routing_adapters::osrm::OsrmResponseParser;
use crate::routing_adapters::valhalla::ValhallaHttpRequestGenerator;
use std::sync::Arc;

use routing_adapters::{RouteRequestGenerator, RouteResponseParser};

uniffi::setup_scaffolding!();

//
// Helpers that are only exposed via the FFI interface.
//
// Most of these exist for convenience since the UDL understandably isn't implementing a
// full Rust type system, and it would be a bunch of boilerplate to expose the full objects.
// Instead, we use top-level functions to return dynamic objects conforming to the trait.
//

/// Creates a [RouteRequestGenerator]
/// which generates requests to an arbitrary Valhalla server (using the OSRM response format).
///
/// This is provided as a convenience for use from foreign code when creating your own [routing_adapters::RouteAdapter].
#[uniffi::export]
fn create_valhalla_request_generator(
    endpoint_url: String,
    profile: String,
) -> Arc<dyn RouteRequestGenerator> {
    Arc::new(ValhallaHttpRequestGenerator::new(endpoint_url, profile))
}

/// Creates a [RouteResponseParser] capable of parsing OSRM responses.
///
/// This response parser is designed to be fairly flexible,
/// supporting both vanilla OSRM and enhanced Valhalla (ex: from Stadia Maps and Mapbox) outputs
/// which contain richer information like banners and voice instructions for navigation.
#[uniffi::export]
fn create_osrm_response_parser(polyline_precision: u32) -> Arc<dyn RouteResponseParser> {
    Arc::new(OsrmResponseParser::new(polyline_precision))
}
