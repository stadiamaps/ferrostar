use crate::models::{BoundingBox, GeographicCoordinate, Route, RouteStep, Waypoint, WaypointKind};
use crate::routing_adapters::{osrm::OsrmResponseParser, RouteResponseParser};
#[cfg(feature = "alloc")]
use alloc::string::ToString;
use geo::{point, BoundingRect, Distance, Haversine, LineString, Point};

// A longer + more complex route
const VALHALLA_EXTENDED_OSRM_RESPONSE: &str =
    include_str!("fixtures/valhalla_extended_osrm_response.json");

// A self-intersecting route
const VALHALLA_SELF_INTERSECTING_OSRM_RESPONSE: &str =
    include_str!("fixtures/valhalla_self_intersecting_osrm_response.json");

/// Gets a longer + more complex route.
///
/// The accuracy of each parser is tested separately in the routing_adapters module;
/// this function simply returns a route for an extended test of the state machine.
pub fn get_extended_route() -> Route {
    let parser = OsrmResponseParser::new(6);
    parser
        .parse_response(VALHALLA_EXTENDED_OSRM_RESPONSE.into())
        .expect("Unable to parse OSRM response")
        .pop()
        .expect("Expected at least one route")
}

/// Gets a self-intersecting route.
///
/// The accuracy of each parser is tested separately in the routing_adapters module;
/// this function simply returns a route for an extended test of the state machine.
pub fn get_self_intersecting_route() -> Route {
    let parser = OsrmResponseParser::new(6);
    parser
        .parse_response(VALHALLA_SELF_INTERSECTING_OSRM_RESPONSE.into())
        .expect("Unable to parse OSRM response")
        .pop()
        .expect("Expected at least one route")
}

pub fn gen_dummy_route_step(
    start_lng: f64,
    start_lat: f64,
    end_lng: f64,
    end_lat: f64,
) -> RouteStep {
    RouteStep {
        geometry: vec![
            GeographicCoordinate {
                lng: start_lng,
                lat: start_lat,
            },
            GeographicCoordinate {
                lng: end_lng,
                lat: end_lat,
            },
        ],
        distance: Haversine.distance(
            point!(x: start_lng, y: start_lat),
            point!(x: end_lng, y: end_lat),
        ),
        duration: 0.0,
        road_name: None,
        exits: vec![],
        instruction: "".to_string(),
        visual_instructions: vec![],
        spoken_instructions: vec![],
        annotations: None,
        incidents: vec![],
    }
}

pub fn gen_route_from_steps(steps: Vec<RouteStep>) -> Route {
    let geometry: Vec<_> = steps
        .iter()
        .flat_map(|step| step.geometry.clone())
        .collect();
    let linestring = LineString::from_iter(geometry.iter().map(|point| Point::from(*point)));
    let distance = steps.iter().fold(0.0, |acc, step| acc + step.distance);
    let bbox = linestring.bounding_rect().unwrap();

    Route {
        geometry,
        bbox: BoundingBox {
            sw: GeographicCoordinate::from(bbox.min()),
            ne: GeographicCoordinate::from(bbox.max()),
        },
        distance,
        waypoints: vec![
            // This method cannot be used outside the test configuration,
            // so unwraps are OK.
            Waypoint {
                coordinate: steps.first().unwrap().geometry.first().cloned().unwrap(),
                kind: WaypointKind::Break,
            },
            Waypoint {
                coordinate: steps.last().unwrap().geometry.last().cloned().unwrap(),
                kind: WaypointKind::Break,
            },
        ],
        steps,
    }
}
