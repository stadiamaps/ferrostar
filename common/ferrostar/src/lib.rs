//! # Ferrostar
//!
//! Ferrostar is a modern SDK for building turn-by-turn navigation applications.
//!
//! ![A screenshot of Ferrostar running on iOS](https://docs.stadiamaps.com/img/ferrostar-screenshot.png)
//!
//! This crate is the [core](https://stadiamaps.github.io/ferrostar/architecture.html) of Ferrostar,
//! which contains common data models, traits and integrations with common routing backends
//! like Valhalla, spatial algorithms, and the navigation state machine.
//!
//! If you're looking to build a navigation experience for a platform
//! where Ferrostar doesn't currently offer a high-level interface,
//! you want to use the primitives in your existing architecture,
//! or you're genuinely curious about the internals,
//! you're in the right spot!
//!
//! And if you're a developer wanting to build a navigation experience in your application,
//! check out the [User Guide](https://stadiamaps.github.io/ferrostar/) for a high-level overview
//! and tutorials for major platforms including iOS and Android.

#![cfg_attr(not(feature = "std"), no_std)]

#[cfg(feature = "alloc")]
extern crate alloc;

#[cfg(target_os = "android")]
use android_logger::{Config, FilterBuilder};

pub mod algorithms;
pub mod deviation_detection;
pub mod models;
pub mod navigation_controller;
pub mod routing_adapters;
pub mod simulation;

#[cfg(target_os = "android")]
fn init_logger() {
    android_logger::init_once(
        Config::default()
            .with_max_level(log::LevelFilter::Trace)
            .with_tag("ferrostar.core"),
    );
    log::info!("Ferrostar android logger initialized");
}

#[cfg(not(target_os = "android"))]
fn init_logger() {
    // Add fallback logging initialization for non-Android platforms
}

#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn create_ferrostar_logger() {
    init_logger();
}

#[cfg(feature = "uniffi")]
mod uniffi_deps {
    pub use crate::models::{Route, Waypoint};
    pub use crate::routing_adapters::{
        error::{InstantiationError, ParsingError},
        osrm::{
            models::{Route as OsrmRoute, Waypoint as OsrmWaypoint},
            OsrmResponseParser,
        },
        valhalla::ValhallaHttpRequestGenerator,
        RouteRequestGenerator, RouteResponseParser,
    };
    pub use chrono::{DateTime, Utc};
    pub use std::{str::FromStr, sync::Arc};
    pub use uniffi::deps::anyhow::anyhow;
    pub use uuid::Uuid;
}
#[cfg(feature = "uniffi")]
#[allow(clippy::wildcard_imports)]
use uniffi_deps::*;

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

// Silliness to keep the macro happy; TODO: open a ticket
#[cfg(feature = "uniffi")]
type UtcDateTime = DateTime<Utc>;

#[cfg(feature = "uniffi")]
uniffi::custom_type!(UtcDateTime, i64);

#[cfg(feature = "uniffi")]
impl UniffiCustomTypeConverter for DateTime<Utc> {
    type Builtin = i64;

    fn into_custom(val: Self::Builtin) -> uniffi::Result<Self> {
        Self::from_timestamp_millis(val).ok_or(anyhow!("Timestamp {val} out of range"))
    }

    fn from_custom(obj: Self) -> Self::Builtin {
        obj.timestamp_millis()
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
    options_json: Option<String>,
) -> Result<Arc<dyn RouteRequestGenerator>, InstantiationError> {
    Ok(Arc::new(ValhallaHttpRequestGenerator::with_options_json(
        endpoint_url,
        profile,
        options_json.as_deref(),
    )?))
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

// MARK: OSRM Route Conversion

/// Creates a [`Route`] from OSRM data.
///
/// This uses the same logic as the [`OsrmResponseParser`] and is designed to be fairly flexible,
/// supporting both vanilla OSRM and enhanced Valhalla (ex: from Stadia Maps and Mapbox) outputs
/// which contain richer information like banners and voice instructions for navigation.
#[cfg(feature = "uniffi")]
#[uniffi::export]
fn create_route_from_osrm(
    route_data: &[u8],
    waypoint_data: &[u8],
    polyline_precision: u32,
) -> Result<Route, ParsingError> {
    let route: OsrmRoute = serde_json::from_slice(route_data)?;
    let waypoints: Vec<OsrmWaypoint> = serde_json::from_slice(waypoint_data)?;
    Route::from_osrm(&route, &waypoints, polyline_precision)
}

/// Creates a [`Route`] from OSRM route data and ferrostar waypoints.
///
/// This uses the same logic as the [`OsrmResponseParser`] and is designed to be fairly flexible,
/// supporting both vanilla OSRM and enhanced Valhalla (ex: from Stadia Maps and Mapbox) outputs
/// which contain richer information like banners and voice instructions for navigation.
#[cfg(feature = "uniffi")]
#[uniffi::export]
fn create_route_from_osrm_route(
    route_data: &[u8],
    waypoints: &[Waypoint],
    polyline_precision: u32,
) -> Result<Route, ParsingError> {
    let route: OsrmRoute = serde_json::from_slice(route_data)?;
    Route::from_osrm_with_standard_waypoints(&route, waypoints, polyline_precision)
}
