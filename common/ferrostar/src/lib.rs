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
// full Rust type system and it would be a bunch of boilerplate to expose the foll objects.
// Instead we use top-level functions to return dynamic objects conforming to the trait.
//

#[uniffi::export]
fn create_valhalla_request_generator(
    endpoint_url: String,
    profile: String,
) -> Arc<dyn RouteRequestGenerator> {
    Arc::new(ValhallaHttpRequestGenerator::new(endpoint_url, profile))
}

#[uniffi::export]
fn create_osrm_response_parser(polyline_precision: u32) -> Arc<dyn RouteResponseParser> {
    Arc::new(OsrmResponseParser::new(polyline_precision))
}
