use crate::models::{GeographicCoordinate, RouteStep, UserLocation};
use geo::{
    Closest, ClosestPoint, EuclideanDistance, HaversineDistance, HaversineLength, LineLocatePoint,
    LineString, Point,
};

use crate::navigation_controller::models::{
    StepAdvanceMode, StepAdvanceStatus,
    StepAdvanceStatus::{Advanced, EndOfRoute},
};

#[cfg(test)]
use {
    crate::navigation_controller::test_helpers::gen_dummy_route_step,
    geo::{coord, point},
    proptest::prelude::*,
    std::time::SystemTime,
};

/// Snaps a user location to the closest point on a route line.
pub fn snap_user_location_to_line(location: UserLocation, line: &LineString) -> UserLocation {
    let original_point = Point::from(location);

    snap_point_to_line(&original_point, line).map_or_else(
        || location,
        |snapped| UserLocation {
            coordinates: GeographicCoordinate {
                lng: snapped.x(),
                lat: snapped.y(),
            },
            ..location
        },
    )
}

fn snap_point_to_line(point: &Point, line: &LineString) -> Option<Point> {
    // TODO: Use haversine_closest_point once a new release is cut?
    match line.closest_point(point) {
        Closest::Intersection(snapped) | Closest::SinglePoint(snapped) => {
            let (x, y) = (snapped.x(), snapped.y());
            if x.is_nan()
                || x.is_subnormal()
                || x.is_infinite()
                || y.is_nan()
                || y.is_subnormal()
                || y.is_infinite()
            {
                None
            } else {
                Some(snapped)
            }
        }
        Closest::Indeterminate => None,
    }
}

pub fn deviation_from_line(point: &Point, line: &LineString) -> Option<f64> {
    snap_point_to_line(point, line).and_then(|snapped| {
        let distance = snapped.haversine_distance(point);

        if distance.is_nan() || distance.is_infinite() {
            None
        } else {
            Some(distance)
        }
    })
}

fn is_close_enough_to_end_of_linestring(
    current_position: &Point,
    current_step_linestring: &LineString,
    threshold: f64,
) -> bool {
    if let Some(end_coord) = current_step_linestring.coords().last() {
        let end_point = Point::from(*end_coord);
        let distance_to_end = end_point.haversine_distance(current_position);

        distance_to_end <= threshold
    } else {
        false
    }
}

/// Determines whether the navigation controller should complete the current route step
/// and move to the next.
///
/// NOTE: The [UserLocation] should *not* be snapped.
pub fn should_advance_to_next_step(
    current_step_linestring: &LineString,
    next_route_step: Option<&RouteStep>,
    user_location: &UserLocation,
    step_advance_mode: StepAdvanceMode,
) -> bool {
    let current_position = Point::from(user_location.coordinates);

    match step_advance_mode {
        StepAdvanceMode::Manual => false,
        StepAdvanceMode::DistanceToEndOfStep {
            distance,
            minimum_horizontal_accuracy,
        } => {
            if user_location.horizontal_accuracy > minimum_horizontal_accuracy.into() {
                false
            } else {
                is_close_enough_to_end_of_linestring(
                    &current_position,
                    current_step_linestring,
                    distance as f64,
                )
            }
        }
        StepAdvanceMode::RelativeLineStringDistance {
            minimum_horizontal_accuracy,
            automatic_advance_distance,
        } => {
            if user_location.horizontal_accuracy > minimum_horizontal_accuracy.into() {
                false
            } else {
                if let Some(distance) = automatic_advance_distance {
                    // Short-circuit: if we are close to the end of the step, we may advance
                    if is_close_enough_to_end_of_linestring(
                        &current_position,
                        current_step_linestring,
                        distance as f64,
                    ) {
                        return true;
                    }
                }

                if let Some(next_step) = next_route_step {
                    // FIXME: This isn't very efficient to keep doing at the moment
                    let next_step_linestring = next_step.get_linestring();

                    // Try to snap the user's current location to the current step
                    // and next step geometries
                    if let (Some(current_step_closest_point), Some(next_step_closest_point)) = (
                        snap_point_to_line(&current_position, current_step_linestring),
                        snap_point_to_line(&current_position, &next_step_linestring),
                    ) {
                        // If the user's distance to the snapped location on the *next* step is <=
                        // the user's distance to the snapped location on the *current* step,
                        // advance to the next step
                        current_position.haversine_distance(&next_step_closest_point)
                            <= current_position.haversine_distance(&current_step_closest_point)
                    } else {
                        // The user's location couldn't be mapped to a single point on both the current and next step.
                        // Fall back to the distance to end of step mode, which has some graceful fallbacks.
                        // In real-world use, this should only happen for values which are EXTREMELY close together.
                        should_advance_to_next_step(
                            current_step_linestring,
                            None,
                            user_location,
                            StepAdvanceMode::DistanceToEndOfStep {
                                distance: minimum_horizontal_accuracy,
                                minimum_horizontal_accuracy,
                            },
                        )
                    }
                } else {
                    // Trigger arrival when the user gets within a circle of the minimum horizontal accuracy
                    should_advance_to_next_step(
                        current_step_linestring,
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

/// Runs a state machine transformation to advance one step.
///
/// Note that this function is pure and the caller must persist any mutations
/// including dropping a completed step.
/// This function is safe and idempotent in the case that it is accidentally
/// invoked with no remaining steps.
pub(crate) fn advance_step(remaining_steps: &[RouteStep]) -> StepAdvanceStatus {
    // NOTE: The first item is the *current* step, and we want the *next* step.
    match remaining_steps.get(1) {
        Some(new_step) => Advanced {
            step: new_step.clone(),
            linestring: new_step.get_linestring(),
        },
        None => EndOfRoute,
    }
}

fn distance_along(point: &Point, linestring: &LineString) -> Option<f64> {
    // TODO: This logic is definitely wrong, but *might* be a sortof usable starting point?
    let total_length = linestring.haversine_length();
    if total_length == 0.0 {
        return Some(0.0);
    }

    let mut cum_length = 0f64;
    let mut closest_dist_to_point = f64::INFINITY;
    let mut traversed = 0f64;
    for segment in linestring.lines() {
        // Convert to a LineString so we get haversine ops
        let segment_linestring = LineString::from(segment);
        // Compute distance to the line (sadly only Euclidean at this point)
        let segment_distance_to_point = segment.euclidean_distance(point);
        let segment_length = segment_linestring.haversine_length();
        if segment_distance_to_point < closest_dist_to_point {
            let segment_fraction = segment.line_locate_point(point)?; // if any segment has a None fraction, return None
            closest_dist_to_point = segment_distance_to_point;
            traversed = cum_length + segment_fraction * segment_length;
        }
        cum_length += segment_length;
    }
    Some(traversed)
}

/// Computes the distance between a location and the end of the current route step.
/// We assume that input location is pre-snapped to route step's linestring.
pub fn distance_to_end_of_step(
    snapped_location: &Point,
    current_step_linestring: &LineString,
) -> f64 {
    let step_length = current_step_linestring.haversine_length();
    if let Some(traversed) = distance_along(snapped_location, current_step_linestring) {
        step_length - traversed
    } else {
        0.0
    }
}

#[cfg(test)]
proptest! {
    #[test]
    fn snap_point_to_line_intersection(
        x1: f64, y1: f64,
        x2: f64, y2: f64,
    ) {
        let point = point! {
            x: x1,
            y: y1,
        };
        let line = LineString::new(vec! {
            coord! {
                x: x1,
                y: y1,
            },
            coord! {
                x: x2,
                y: y2,
            },
        });

        if let Some(snapped) = snap_point_to_line(&point, &line) {
            let x = snapped.x();
            let y = snapped.y();

            prop_assert!(!x.is_nan());
            prop_assert!(!x.is_subnormal());
            prop_assert!(!x.is_infinite());

            prop_assert!(!y.is_nan());
            prop_assert!(!y.is_subnormal());
            prop_assert!(!y.is_infinite());
        } else {
            // Edge case 1: extremely small differences in values
            let is_miniscule_difference = (x1 - x2).abs() < 0.00000001 || (y1 - y2).abs() < 0.00000001;
            // Edge case 2: Values which are clearly not WGS84 ;)
            let is_non_wgs84 = (x1 - x2).abs() > 180.0 || (y1 - y2).abs() > 90.0;
            prop_assert!(is_miniscule_difference || is_non_wgs84);
        }
    }

    #[test]
    fn should_advance_exact_position(
        x1: f64, y1: f64,
        x2: f64, y2: f64,
        x3: f64, y3: f64,
        has_next_step: bool,
        distance: u16, minimum_horizontal_accuracy: u16, excess_inaccuracy in 0f64..,
        automatic_advance_distance: Option<u16>,
    ) {
        if !(x1 == x2 && y1 == y2) && !(x1 == x3 && y1 == y3) {
            // Guard against:
            //   1. Invalid linestrings
            //   2. Invalid tests (we assume that the route isn't a closed loop)
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
            prop_assert!(!should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &exact_user_location, StepAdvanceMode::Manual));
            prop_assert!(!should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &inaccurate_user_location, StepAdvanceMode::Manual));

            // Always succeeds in the base case in distance to end of step mode
            let cond = should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &exact_user_location, StepAdvanceMode::DistanceToEndOfStep {
                distance, minimum_horizontal_accuracy
            });
            prop_assert!(cond);

            // Same when looking at the relative distances between the two step geometries
            let cond = should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &exact_user_location, StepAdvanceMode::RelativeLineStringDistance {
                minimum_horizontal_accuracy,
                automatic_advance_distance
            });
            prop_assert!(cond);

            // Should always fail (unless excess_inaccuracy is zero), as the horizontal accuracy is worse than (>) than the desired error threshold
            prop_assert_eq!(should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &inaccurate_user_location, StepAdvanceMode::DistanceToEndOfStep {
                distance, minimum_horizontal_accuracy
            }), excess_inaccuracy == 0.0, "Expected that the navigation would not advance to the next step except when excess_inaccuracy is 0");
            prop_assert_eq!(should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &inaccurate_user_location, StepAdvanceMode::RelativeLineStringDistance {
                minimum_horizontal_accuracy,
                automatic_advance_distance
            }), excess_inaccuracy == 0.0, "Expected that the navigation would not advance to the next step except when excess_inaccuracy is 0");
        }
    }

    #[test]
    fn should_advance_inexact_position(
        x1: f64, y1: f64,
        x2: f64, y2: f64,
        x3: f64, y3: f64,
        error in -0.003f64..=0.003f64, has_next_step: bool,
        distance: u16, minimum_horizontal_accuracy: u16,
        automatic_advance_distance: Option<u16>,
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
            coordinates: GeographicCoordinate {
                lng: end_of_step.lng + error,
                lat: end_of_step.lat + error,
            },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
        };
        let user_location_point = Point::from(user_location);
        let distance_from_end_of_current_step = user_location_point.haversine_distance(&end_of_step.into());

        // Never advance to the next step when StepAdvanceMode is Manual
        prop_assert!(!should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &user_location, StepAdvanceMode::Manual));

        // Assumes that underlying distance calculations in GeoRust are correct is correct
        prop_assert_eq!(should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &user_location, StepAdvanceMode::DistanceToEndOfStep {
            distance, minimum_horizontal_accuracy
        }), distance_from_end_of_current_step <= distance.into(), "Expected that the step should advance in this case as we are closer to the end of the step than the threshold.");

        // Similar test for automatic advance on the relative line string distance mode
        if automatic_advance_distance.map_or(false, |advance_distance| {
            distance_from_end_of_current_step <= advance_distance.into()
        }) {
            prop_assert!(should_advance_to_next_step(&current_route_step.get_linestring(), next_route_step.as_ref(), &user_location, StepAdvanceMode::RelativeLineStringDistance {
                minimum_horizontal_accuracy,
                automatic_advance_distance,
            }), "Expected that the step should advance any time that the haversine distance to the end of the step is within the automatic advance threshold.");
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use geo::{coord, point};

    #[test]
    fn test_deviation_from_line() {
        // Diagonal line from the origin to (1,1)
        let linestring = LineString::new(vec![coord! {x: 0.0, y: 0.0}, coord! {x: 1.0, y: 1.0}]);

        let origin = point! {
            x: 0.0,
            y: 0.0,
        };
        let midpoint = point! {
            x: 0.5,
            y: 0.5,
        };
        let off_line = point! {
            x: 1.0,
            y: 0.5,
        };

        // The origin is directly on the line
        assert_eq!(deviation_from_line(&origin, &linestring), Some(0.0));

        // The midpoint is also directly on the line
        assert_eq!(deviation_from_line(&midpoint, &linestring), Some(0.0));

        // This point however is off the line.
        // We assume the underlying library functions are tested and this is just a sanity check.
        assert!(deviation_from_line(&off_line, &linestring)
            .map_or(false, |deviation| deviation - 39312.21257675703
                < f64::EPSILON));
    }
}
// TODO: Unit tests
// - Under and over distance accuracy thresholds
// - Equator and extreme latitude
