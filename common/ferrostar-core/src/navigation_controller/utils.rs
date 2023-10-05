use crate::models::{GeographicCoordinates, RouteStep, UserLocation};
use geo::{Closest, HaversineClosestPoint, HaversineDistance, LineString, Point};
use proptest::prelude::*;

#[cfg(test)]
use std::time::SystemTime;

/// Snaps a user location to the closest point on a route line.
pub fn snap_to_line(location: UserLocation, line: &LineString) -> UserLocation {
    let original_point = Point::new(location.coordinates.lng, location.coordinates.lat);

    match line.haversine_closest_point(&original_point) {
        Closest::Intersection(snapped) | Closest::SinglePoint(snapped) => UserLocation {
            coordinates: GeographicCoordinates {
                lng: snapped.x(),
                lat: snapped.y(),
            },
            ..location
        },
        Closest::Indeterminate => location,
    }
}

/// Determines whether the navigation controller should complete the current route step
/// and move to the next.
///
/// NOTE: The [UserLocation] should *not* be snapped.
pub fn has_completed_step(route_step: &RouteStep, user_location: &UserLocation) -> bool {
    // TODO: this is an extremely simplistic first pass and does not account for edge cases.
    // Future room for improvement:
    //   - Coping with poor GPS accuracy
    //   - Expecting a turn (analyze buffer of recent GPS readings/compass) when the route is supposed to turn

    let end: Point = route_step.end_location.into();
    let current_position: Point = user_location.coordinates.into();
    let distance_to_end = end.haversine_distance(&current_position);

    return distance_to_end < 5.0;
}

proptest! {
    #[test]
    fn test_has_completed_step_exact_position(x1 in -180f64..180f64, y1 in -90f64..90f64,
                                              x2 in -180f64..180f64, y2 in -90f64..90f64) {
        let route_step = RouteStep {
            start_location: GeographicCoordinates { lng: x1, lat: y1 },
            end_location: GeographicCoordinates { lng: x2, lat: y2 },
            distance: 0.0,
            road_name: None,
            instruction: "".to_string(),
        };
        let user_location = UserLocation {
            coordinates: route_step.end_location, // Exactly at the end location
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
        };
        assert!(has_completed_step(&route_step, &user_location));
    }
}
