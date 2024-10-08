use crate::models::{BoundingBox, GeographicCoordinate, Route, RouteStep, Waypoint, WaypointKind};
#[cfg(feature = "alloc")]
use alloc::string::ToString;
use geo::{line_string, BoundingRect, HaversineLength, LineString, Point};

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
        distance: line_string![
            (x: start_lng, y: start_lat),
            (x: end_lng, y: end_lat)
        ]
        .haversine_length(),
        duration: 0.0,
        road_name: None,
        instruction: "".to_string(),
        visual_instructions: vec![],
        spoken_instructions: vec![],
        annotations: None,
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
        congestion_segments: None,
    }
}
