//! # Ferrostar
//!
//! Ferrostar is a modern SDK for building turn-by-turn navigation applications.
//!
//! Check out the [User Guide](https://stadiamaps.github.io/ferrostar/) for an introduction,
//! or poke around here for the public API reference!
//!
//! We apologize for the mess, but should have the documentation in a much better state by version
//! 0.1.0 (est. mid-April).

#![cfg_attr(not(feature = "std"), no_std)]

#[cfg(feature = "alloc")]
extern crate alloc;

// Use `wee_alloc` as the global allocator (used on platforms like wasm)
#[cfg(feature = "wee_alloc")]
#[global_allocator]
static ALLOC: wee_alloc::WeeAlloc = wee_alloc::WeeAlloc::INIT;

pub mod algorithms;
pub mod deviation_detection;
pub mod models;
pub mod navigation_controller;
pub mod routing_adapters;
pub mod simulation;

#[cfg(feature = "uniffi")]
use routing_adapters::{
    error::InstantiationError, osrm::OsrmResponseParser, valhalla::ValhallaHttpRequestGenerator,
    RouteRequestGenerator, RouteResponseParser,
};
#[cfg(feature = "uniffi")]
use std::{str::FromStr, sync::Arc};
#[cfg(feature = "uniffi")]
use uuid::Uuid;

#[cfg(feature = "uniffi")]
uniffi::setup_scaffolding!();

#[cfg(feature = "uniffi")]
uniffi::custom_type!(Uuid, String);

#[cfg(feature = "uniffi")]
impl UniffiCustomTypeConverter for Uuid {
    type Builtin = String;

    fn into_custom(val: Self::Builtin) -> uniffi::Result<Self> {
        Ok(Uuid::from_str(&val)?)
    }

    fn from_custom(obj: Self) -> Self::Builtin {
        obj.to_string()
    }
}

//
// Helpers that are only exposed via the FFI interface.
//
// Most of these exist for convenience since the UDL understandably isn't implementing a
// full Rust type system, and it would be a bunch of boilerplate to expose the full objects.
// Instead, we use top-level functions to return dynamic objects conforming to the trait.
//

/// Creates a [`RouteRequestGenerator`]
/// which generates requests to an arbitrary Valhalla server (using the OSRM response format).
///
/// This is provided as a convenience for use from foreign code when creating your own [`routing_adapters::RouteAdapter`].
#[cfg(feature = "uniffi")]
#[uniffi::export]
fn create_valhalla_request_generator(
    endpoint_url: String,
    profile: String,
    costing_options_json: Option<String>,
) -> Result<Arc<dyn RouteRequestGenerator>, InstantiationError> {
    Ok(Arc::new(
        ValhallaHttpRequestGenerator::with_costing_options_json(
            endpoint_url,
            profile,
            costing_options_json,
        )?,
    ))
}

/// Creates a [`RouteResponseParser`] capable of parsing OSRM responses.
///
/// This response parser is designed to be fairly flexible,
/// supporting both vanilla OSRM and enhanced Valhalla (ex: from Stadia Maps and Mapbox) outputs
/// which contain richer information like banners and voice instructions for navigation.
#[cfg(feature = "uniffi")]
#[uniffi::export]
fn create_osrm_response_parser(polyline_precision: u32) -> Arc<dyn RouteResponseParser> {
    Arc::new(OsrmResponseParser::new(polyline_precision))
}
