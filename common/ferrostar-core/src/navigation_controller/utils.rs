use crate::models::{GeographicCoordinates, RouteStep, UserLocation};
use geo::{Closest, ClosestPoint, Coord, HaversineDistance, LineString, Point};

#[cfg(test)]
use proptest::prelude::*;

use super::ARRIVED_EOT;
use crate::{NavigationStateUpdate, StepAdvanceMode};
#[cfg(test)]
use std::time::SystemTime;

/// Snaps a user location to the closest point on a route line.
pub fn snap_user_location_to_line(location: &UserLocation, line: &LineString) -> UserLocation {
    let original_point = Point::new(location.coordinates.lng, location.coordinates.lat);

    snap_point_to_line(&original_point, line).map_or_else(
        || *location,
        |snapped| UserLocation {
            coordinates: GeographicCoordinates {
                lng: snapped.x(),
                lat: snapped.y(),
            },
            ..*location
        },
    )
}

fn snap_point_to_line(point: &Point, line: &LineString) -> Option<Point> {
    if point.intersects(line) {
        // This branch is necessary for the moment due to https://github.com/georust/geo/issues/1084
        Some(*point)
    } else {
        match line.haversine_closest_point(point) {
            Closest::Intersection(snapped) | Closest::SinglePoint(snapped) => Some(snapped),
            Closest::Indeterminate => None,
        }
    }
}

/// Determines whether the navigation controller should complete the current route step
/// and move to the next.
///
/// NOTE: The [UserLocation] should *not* be snapped.
pub fn should_advance_to_next_step(
    current_route_step: &RouteStep,
    next_route_step: Option<&RouteStep>,
    user_location: &UserLocation,
    step_advance_mode: StepAdvanceMode,
) -> bool {
    let current_position: Point = user_location.coordinates.into();

    match step_advance_mode {
        StepAdvanceMode::Manual => false,
        StepAdvanceMode::DistanceToEndOfStep {
            distance,
            minimum_horizontal_accuracy,
        } => {
            if user_location.horizontal_accuracy > minimum_horizontal_accuracy.into() {
                false
            } else if let Some(end_coord) = current_route_step.geometry.last() {
                let end_point: Point = (*end_coord).into();
                let distance_to_end = end_point.haversine_distance(&current_position);

                distance_to_end <= distance as f64
            } else {
                false
            }
        }
        StepAdvanceMode::RelativeLineStringDistance {
            minimum_horizontal_accuracy,
        } => {
            if user_location.horizontal_accuracy > minimum_horizontal_accuracy.into() {
                false
            } else {
                if let Some(next_step) = next_route_step {
                    // TODO: This is not very efficient to do every step; store/cache this
                    let current_step_linestring =
                        LineString::from_iter(current_route_step.geometry.iter().map(|coord| {
                            Coord {
                                x: coord.lng,
                                y: coord.lat,
                            }
                        }));
                    let next_step_linestring =
                        LineString::from_iter(next_step.geometry.iter().map(|coord| Coord {
                            x: coord.lng,
                            y: coord.lat,
                        }));

                    // Try to snap the user's current location to the current step
                    // and next step geometries
                    if let (Some(current_step_closest_point), Some(next_step_closest_point)) = (
                        snap_point_to_line(&current_position, &current_step_linestring),
                        snap_point_to_line(&current_position, &next_step_linestring),
                    ) {
                        // If the user's distance to the snapped location on the *next* step is <=
                        // the user's distance to the snapped location on the *current* step,
                        // advance to the next step
                        current_position.haversine_distance(&next_step_closest_point)
                            <= current_position.haversine_distance(&current_step_closest_point)
                    } else {
                        // The user's location couldn't be mapped to a single point on both the current and next step
                        false
                    }
                } else {
                    // Trigger arrival when the user gets within a circle of the minimum horizontal accuracy
                    should_advance_to_next_step(
                        current_route_step,
                        None,
                        user_location,
                        StepAdvanceMode::DistanceToEndOfStep {
                            distance: minimum_horizontal_accuracy,
                            minimum_horizontal_accuracy,
                        },
                    )
                }
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
fn gen_dummy_route_step(start_lng: f64, start_lat: f64, end_lng: f64, end_lat: f64) -> RouteStep {
    RouteStep {
        geometry: vec![
            GeographicCoordinates {
                lng: start_lng,
                lat: start_lat,
            },
            GeographicCoordinates {
                lng: end_lng,
                lat: end_lat,
            },
        ],
        distance: 0.0,
        road_name: None,
        instruction: "".to_string(),
        visual_instructions: vec![],
    }
}

#[cfg(test)]
proptest! {
    #[test]
    fn should_advance_exact_position(
        x1 in -180f64..180f64, y1 in -90f64..90f64,
        x2 in -180f64..180f64, y2 in -90f64..90f64,
        x3 in -180f64..180f64, y3 in -90f64..90f64,
        has_next_step: bool,
        distance: u16, minimum_horizontal_accuracy: u16, excess_inaccuracy in 0f64..65535f64
    ) {
        let current_route_step = gen_dummy_route_step(x1, y1, x2, y2);
        let next_route_step = if has_next_step {
            Some(gen_dummy_route_step(x2, y2, x3, y3))
        } else {
            None
        };
        let exact_user_location = UserLocation {
            coordinates: *current_route_step.geometry.last().unwrap(), // Exactly at the end location
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
        };

        let inaccurate_user_location = UserLocation {
            horizontal_accuracy: (minimum_horizontal_accuracy as f64) + excess_inaccuracy,
            ..exact_user_location
        };

        // Never advance to the next step when StepAdvanceMode is Manual
        assert!(!should_advance_to_next_step(&current_route_step, next_route_step.as_ref(), &exact_user_location, StepAdvanceMode::Manual));
        assert!(!should_advance_to_next_step(&current_route_step, next_route_step.as_ref(), &inaccurate_user_location, StepAdvanceMode::Manual));

        // Always succeeds in the base case in distance to end of step mode
        assert!(should_advance_to_next_step(&current_route_step, next_route_step.as_ref(), &exact_user_location, StepAdvanceMode::DistanceToEndOfStep {
            distance, minimum_horizontal_accuracy
        }));

        // Same when looking at the relative distances between the two step geometries
        assert!(should_advance_to_next_step(&current_route_step, next_route_step.as_ref(), &exact_user_location, StepAdvanceMode::RelativeLineStringDistance {
            minimum_horizontal_accuracy
        }));

        // Should always fail (unless excess_inaccuracy is zero), as the horizontal accuracy is worse than (>) than the desired error threshold
        assert_eq!(should_advance_to_next_step(&current_route_step, next_route_step.as_ref(), &inaccurate_user_location, StepAdvanceMode::DistanceToEndOfStep {
            distance, minimum_horizontal_accuracy
        }), excess_inaccuracy == 0.0, "Expected that the navigation would not advance to the next step except when excess_inaccuracy is 0");
        assert_eq!(should_advance_to_next_step(&current_route_step, next_route_step.as_ref(), &inaccurate_user_location, StepAdvanceMode::RelativeLineStringDistance {
            minimum_horizontal_accuracy
        }), excess_inaccuracy == 0.0, "Expected that the navigation would not advance to the next step except when excess_inaccuracy is 0");
    }

    #[test]
    fn should_advance_inexact_position(
        x1 in -180f64..180f64, y1 in -90f64..90f64,
        x2 in -180f64..180f64, y2 in -90f64..90f64,
        x3 in -180f64..180f64, y3 in -90f64..90f64,
        error in -0.003f64..0.003f64, has_next_step: bool,
        distance: u16, minimum_horizontal_accuracy in 0u16..250u16
    ) {
        let current_route_step = gen_dummy_route_step(x1, y1, x2, y2);
        let next_route_step = if has_next_step {
            Some(gen_dummy_route_step(x2, y2, x3, y3))
        } else {
            None
        };

        // Construct a user location that's slightly offset from the transition point with perfect accuracy
        let end_of_step = *current_route_step.geometry.last().unwrap();
        let user_location = UserLocation {
            coordinates: GeographicCoordinates {
                lng: end_of_step.lng + error,
                lat: end_of_step.lat + error,
            },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
        };
        let user_location_point: Point = user_location.into();
        // let distance_from_end_of_current_step = user_location.into().

        // Never advance to the next step when StepAdvanceMode is Manual
        assert!(!should_advance_to_next_step(&current_route_step, next_route_step.as_ref(), &user_location, StepAdvanceMode::Manual));

        // Assumes that haversine_distance is correct
        assert_eq!(should_advance_to_next_step(&current_route_step, next_route_step.as_ref(), &user_location, StepAdvanceMode::DistanceToEndOfStep {
            distance, minimum_horizontal_accuracy
        }), user_location_point.haversine_distance(&end_of_step.into()) <= distance.into(), "Expected that the step should advance in this case as we are closer to the end of the step than the threshold.");

        // We can use snap_to_line and, assuming that snap_to_line works and haversine_distance works, we have a valid end to end test
    }
}

// TODO: Unit tests
// - Under and over distance accuracy thresholds
// - Equator and extreme latitude
