use crate::models::{GeographicCoordinates, RouteStep, UserLocation};
use geo::{Closest, HaversineClosestPoint, HaversineDistance, LineString, Point};

#[cfg(test)]
use proptest::prelude::*;

use super::ARRIVED_EOT;
use crate::{NavigationStateUpdate, StepAdvanceMode};
#[cfg(test)]
use std::time::SystemTime;

/// Snaps a user location to the closest point on a route line.
pub fn snap_to_line(location: &UserLocation, line: &LineString) -> UserLocation {
    let original_point = Point::new(location.coordinates.lng, location.coordinates.lat);

    match line.haversine_closest_point(&original_point) {
        Closest::Intersection(snapped) | Closest::SinglePoint(snapped) => UserLocation {
            coordinates: GeographicCoordinates {
                lng: snapped.x(),
                lat: snapped.y(),
            },
            ..*location
        },
        Closest::Indeterminate => *location,
    }
}

/// Determines whether the navigation controller should complete the current route step
/// and move to the next.
///
/// NOTE: The [UserLocation] should *not* be snapped.
pub fn should_advance_to_next_step(
    route_step: &RouteStep,
    user_location: &UserLocation,
    step_advance_mode: StepAdvanceMode,
) -> bool {
    // Future room for improvement:
    //   - Coping with poor GPS accuracy
    //   - Expecting a turn (analyze buffer of recent GPS readings/compass) when the route is supposed to turn

    match step_advance_mode {
        StepAdvanceMode::Manual => false,
        StepAdvanceMode::DistanceToLastWaypoint {
            distance,
            minimum_horizontal_accuracy,
        } => {
            if user_location.horizontal_accuracy > minimum_horizontal_accuracy.into() {
                false
            } else {
                let end: Point = route_step.end_location.into();
                let current_position: Point = user_location.coordinates.into();
                let distance_to_end = end.haversine_distance(&current_position);

                distance_to_end < 5.0
            }
        }
    }
}

pub fn do_advance_to_next_step(
    snapped_user_location: &UserLocation,
    remaining_waypoints: &Vec<GeographicCoordinates>,
    remaining_steps: &mut Vec<RouteStep>,
) -> NavigationStateUpdate {
    if remaining_steps.is_empty() {
        return ARRIVED_EOT;
    };

    // Advance to the next step
    let current_step = if !remaining_steps.is_empty() {
        // NOTE: this would be much more efficient if we used a VecDeque, but
        // that isn't bridged by UniFFI. Revisit later.
        remaining_steps.remove(0);
        remaining_steps.first()
    } else {
        None
    };

    if let Some(step) = current_step {
        NavigationStateUpdate::Navigating {
            snapped_user_location: *snapped_user_location,
            remaining_waypoints: remaining_waypoints.clone(),
            current_step: step.clone(),
            spoken_instruction: None,
            visual_instructions: None,
        }
    } else {
        ARRIVED_EOT
    }
}

#[cfg(test)]
proptest! {
    #[test]
    fn test_should_advance_exact_position(x1 in -180f64..180f64, y1 in -90f64..90f64,
                                          x2 in -180f64..180f64, y2 in -90f64..90f64,
                                          distance: u16, minimum_horizontal_accuracy: u16,
                                          excess_inaccuracy in 0f64..65535f64) {
        let route_step = RouteStep {
            start_location: GeographicCoordinates { lng: x1, lat: y1 },
            end_location: GeographicCoordinates { lng: x2, lat: y2 },
            distance: 0.0,
            road_name: None,
            instruction: "".to_string(),
        };
        let exact_user_location = UserLocation {
            coordinates: route_step.end_location, // Exactly at the end location
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
        };

        let inaccurate_user_location = UserLocation {
            horizontal_accuracy: (minimum_horizontal_accuracy as f64) + excess_inaccuracy,
            ..exact_user_location
        };

        // Always succeeds in the base case
        assert!(should_advance_to_next_step(&route_step, &exact_user_location, StepAdvanceMode::DistanceToLastWaypoint {
            distance, minimum_horizontal_accuracy
        }));

        // Should always fail since the min horizontal accuracy is > than the desired amount
        assert_eq!(should_advance_to_next_step(&route_step, &inaccurate_user_location, StepAdvanceMode::DistanceToLastWaypoint {
            distance, minimum_horizontal_accuracy
        }), excess_inaccuracy == 0.0, "Expected that the navigation would not advance to the next step except when excess_inaccuracy == 0.");

        // Never advance to the next step when StepAdvanceMode is Manual
        assert!(!should_advance_to_next_step(&route_step, &exact_user_location, StepAdvanceMode::Manual));
    }
}
