use crate::models::{BoundingBox, GeographicCoordinate, Route, RouteStep, Waypoint, WaypointKind};
use crate::routing_adapters::{osrm::OsrmResponseParser, RouteResponseParser};
#[cfg(feature = "alloc")]
use alloc::string::ToString;
use chrono::{DateTime, Utc};
use geo::{point, BoundingRect, Coord, Distance, Haversine, LineString, Point};
use insta::{dynamic_redaction, Settings};

pub enum TestRoute {
    /// Gets a longer + more complex route.
    Extended,
    /// Gets a self-intersecting route.
    SelfIntersecting,
}

impl TestRoute {
    pub fn file_content(&self) -> &'static str {
        match self {
            TestRoute::Extended => include_str!("fixtures/valhalla_extended_osrm_response.json"),
            TestRoute::SelfIntersecting => {
                include_str!("fixtures/valhalla_self_intersecting_osrm_response.json")
            }
        }
    }
}

/// The accuracy of each parser is tested separately in the routing_adapters module;
/// this function simply returns a route for an extended test of the state machine.
pub fn get_test_route(test_route: TestRoute) -> Route {
    let parser = OsrmResponseParser::new(6);
    parser
        .parse_response(test_route.file_content().into())
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

/// Creates a RouteStep with a list of coordinates.
///
/// This is useful for testing specific scenarios where you need more control over the
/// route geometry.
///
/// # Arguments
///
/// * `coordinates` - A vector of coordinates
pub fn gen_route_step_with_coords(coordinates: Vec<Coord>) -> RouteStep {
    if coordinates.len() < 2 {
        panic!("A route step requires at least 2 coordinates");
    }

    let geo_coordinates: Vec<GeographicCoordinate> = coordinates
        .into_iter()
        .map(|coord| GeographicCoordinate {
            lng: coord.x,
            lat: coord.y,
        })
        .collect();

    // Calculate the total distance along the route
    let points: Vec<Point> = geo_coordinates
        .iter()
        .map(|coord| point!(x: coord.lng, y: coord.lat))
        .collect();

    let mut total_distance = 0.0;
    for i in 0..points.len() - 1 {
        total_distance += Haversine.distance(points[i], points[i + 1]);
    }

    RouteStep {
        geometry: geo_coordinates,
        distance: total_distance,
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

fn create_timestamp_redaction(
) -> impl Fn(insta::internals::Content, insta::internals::ContentPath<'_>) -> &'static str
       + Send
       + Sync
       + 'static {
    |value, _path| {
        if value.is_nil() {
            "[none]"
        } else if let Some(timestamp_str) = value.as_str() {
            match timestamp_str.parse::<DateTime<Utc>>() {
                Ok(_) => "[timestamp]",
                Err(_) => "[invalid-timestamp]",
            }
        } else if let Some(timestamp_num) = value.as_i64() {
            match DateTime::<Utc>::from_timestamp_millis(timestamp_num) {
                Some(_) => "[timestamp]",
                None => "[invalid-timestamp]",
            }
        } else {
            "[unexpected-value]"
        }
    }
}

fn create_distance_redaction(
) -> impl Fn(insta::internals::Content, insta::internals::ContentPath<'_>) -> String
       + Send
       + Sync
       + 'static {
    |value, _path| {
        if value.is_nil() {
            "[none]".to_string()
        } else if let Some(distance_str) = value.as_str() {
            match distance_str.parse::<f64>() {
                Ok(distance) => {
                    // Round to 10 decimal places
                    format!("{:.10}", distance)
                }
                Err(_) => "[invalid-distance]".to_string(),
            }
        } else if let Some(distance) = value.as_f64() {
            // Round to 10 decimal places
            format!("{:.10}", distance)
        } else {
            "[unexpected-value]".to_string()
        }
    }
}

pub(crate) fn nav_controller_insta_settings() -> Settings {
    let mut settings = Settings::new();
    settings.add_redaction(
        ".**.startedAt",
        dynamic_redaction(create_timestamp_redaction()),
    );
    settings.add_redaction(
        ".**.endedAt",
        dynamic_redaction(create_timestamp_redaction()),
    );
    settings.add_redaction(
        ".**.timestamp",
        dynamic_redaction(create_timestamp_redaction()),
    );
    settings.add_redaction(
        ".**.initial_timestamp",
        dynamic_redaction(create_timestamp_redaction()),
    );

    settings.add_redaction(
        ".**.distanceTraveled",
        dynamic_redaction(create_distance_redaction()),
    );
    settings.add_redaction(
        ".**.snappedDistanceTraveled",
        dynamic_redaction(create_distance_redaction()),
    );
    settings.add_redaction(
        ".**.distanceToNextManeuver",
        dynamic_redaction(create_distance_redaction()),
    );
    settings.add_redaction(
        ".**.distanceRemaining",
        dynamic_redaction(create_distance_redaction()),
    );
    settings.add_redaction(
        ".**.durationRemaining",
        dynamic_redaction(create_distance_redaction()),
    );

    settings
}
