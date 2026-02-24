use crate::models::{GeographicCoordinate, Route, UserLocation};
use crate::routing_adapters::{RouteResponseParser, osrm::OsrmResponseParser};
use geo::{Coord, coord};
use proptest::prop_compose;

use insta::_macro_support::Content;
use insta::internals::ContentPath;
use serde::Serialize;
use serde::de::DeserializeOwned;
#[cfg(all(feature = "std", not(feature = "web-time")))]
use std::time::SystemTime;
#[cfg(feature = "web-time")]
use web_time::SystemTime;

pub fn make_user_location(coord: Coord, horizontal_accuracy: f64) -> UserLocation {
    UserLocation {
        coordinates: GeographicCoordinate {
            lat: coord.y,
            lng: coord.x,
        },
        horizontal_accuracy,
        course_over_ground: None,
        timestamp: SystemTime::now(),
        speed: None,
    }
}

prop_compose! {
    pub fn arb_coord()(x in -180f64..180f64, y in -90f64..90f64) -> Coord {
        coord! {x: x, y: y}
    }
}

prop_compose! {
    pub fn arb_user_loc(horizontal_accuracy: f64)(coord in arb_coord()) -> UserLocation {
        make_user_location(coord, horizontal_accuracy)
    }
}

/// Named test fixture routes that can be loaded from the shared `fixtures/` directory.
///
/// Each variant corresponds to a JSON fixture file containing an OSRM-compatible route response.
pub enum TestRoute {
    /// Standard OSRM polyline6 response.
    StandardOsrm,
    /// Valhalla OSRM response.
    Valhalla,
    /// Valhalla OSRM response with via waypoints.
    ValhallaViaWays,
    /// Valhalla extended OSRM response (includes sub-maneuvers/lane info).
    ValhallaExtended,
    /// Valhalla OSRM response with exit info.
    ValhallaWithExits,
    /// Valhalla OSRM response with roundabouts (left-hand driving, UK).
    ValhallaWithRoundabouts,
    /// Valhalla self-intersecting route.
    ValhallaSelfIntersecting,
}

impl TestRoute {
    /// Returns the raw JSON string for this fixture.
    pub fn file_content(&self) -> &'static str {
        match self {
            TestRoute::StandardOsrm => {
                include_str!("fixtures/standard_osrm_polyline6_response.json")
            }
            TestRoute::Valhalla => {
                include_str!("fixtures/valhalla_osrm_response.json")
            }
            TestRoute::ValhallaViaWays => {
                include_str!("fixtures/valhalla_osrm_response_via_ways.json")
            }
            TestRoute::ValhallaExtended => {
                include_str!("fixtures/valhalla_extended_osrm_response.json")
            }
            TestRoute::ValhallaWithExits => {
                include_str!("fixtures/valhalla_osrm_response_with_exit_info.json")
            }
            TestRoute::ValhallaWithRoundabouts => {
                include_str!("fixtures/valhalla_osrm_with_roundabouts.json")
            }
            TestRoute::ValhallaSelfIntersecting => {
                include_str!("fixtures/valhalla_self_intersecting_osrm_response.json")
            }
        }
    }

    /// Parses the fixture into a vector of routes.
    pub fn parse(&self) -> Vec<Route> {
        let parser = OsrmResponseParser::new(6);
        parser
            .parse_response(self.file_content().into())
            .expect("Unable to parse test route fixture")
    }

    /// Parses the fixture and returns the first (or only) route.
    pub fn first_route(&self) -> Route {
        self.parse()
            .pop()
            .expect("Expected at least one route in fixture")
    }
}

/// An insta redaction that parses property bytes as a generic type and returns a JSON string.
///
/// This enables both validation and easier diffing.
pub fn redact_properties<T: DeserializeOwned + Serialize>(
    value: Content,
    _path: ContentPath,
) -> String {
    // Deserialize to properties (so we know it's in the right format!)
    let content_slice = value.as_slice().expect("Unable to get content as slice");
    let content_bytes: Vec<_> = content_slice
        .iter()
        .map(|c| {
            let c64 = c.as_u64().expect("Could not get content value as a number");
            u8::try_from(c64).expect("Unexpected byte value")
        })
        .collect();
    let result: T = serde_json::from_slice(&content_bytes)
        .expect("Unable to deserialize as OsrmWaypointProperties");
    serde_json::to_string(&result).expect("Unable to serialize")
}
