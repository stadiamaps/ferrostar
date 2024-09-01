//! Common spatial algorithms which are useful for navigation.

use crate::navigation_controller::models::{
    StepAdvanceMode, StepAdvanceStatus,
    StepAdvanceStatus::{Advanced, EndOfRoute},
};
use crate::{
    models::{GeographicCoordinate, RouteStep, UserLocation},
    navigation_controller::models::TripProgress,
};
use geo::{
    Closest, ClosestPoint, Coord, EuclideanDistance, HaversineDistance, HaversineLength,
    LineLocatePoint, LineString, Point,
};

#[cfg(test)]
use {
    crate::navigation_controller::test_helpers::gen_dummy_route_step,
    geo::{coord, point},
    proptest::prelude::*,
};

#[cfg(all(test, feature = "std", not(feature = "web-time")))]
use std::time::SystemTime;

#[cfg(all(test, feature = "web-time"))]
use web_time::SystemTime;

/// Get the index of the closest point in the line.
pub fn index_of_closest_origin_point(
    location: UserLocation,
    line: &LineString,
    skip_to_index: u64,
) -> u64 {
    let max_index = line.coords().count() - 1;
    if skip_to_index >= max_index as u64 {
        return max_index as u64;
    }

    let point = Point::from(location.coordinates);
    let skip_index = skip_to_index as usize;

    line.lines()
        .enumerate()
        .skip(skip_index)
        .min_by(|(_, line1), (_, line2)| {
            let dist1 = line1.euclidean_distance(&point);
            let dist2 = line2.euclidean_distance(&point);
            dist1.partial_cmp(&dist2).unwrap()
        })
        .map(|(index, _)| index as u64)
        .unwrap()
}

/// Snaps a user location to the closest point on a route line.
///
/// If the location cannot be snapped (should only be possible with an invalid coordinate or geometry),
/// the location is returned unaltered.
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

/// Internal function that truncates a float to 6 digits.
///
/// Note that this approach is not a substitute for fixed precision decimals,
/// but it is acceptable for our use case,
/// where our main goal is to avoid precision issues for values which do not matter
/// and remove most edge cases with floating point numbers.
///
/// The `decimal_digits` parameter refers to the number of digits after the point.
pub(crate) fn trunc_float(value: f64, decimal_digits: u32) -> f64 {
    let factor = 10_i64.pow(decimal_digits) as f64;
    (value * factor).round() / factor
}

/// Predicate which is used to filter out several types of undesirable floating point values.
///
/// These include NaN values, subnormals (usually the result of underflow), and infinite values.
fn is_valid_float(value: f64) -> bool {
    !value.is_nan() && !value.is_subnormal() && !value.is_infinite()
}

fn snap_point_to_line(point: &Point, line: &LineString) -> Option<Point> {
    // Bail early when we have two essentially identical points.
    // This can cause some issues with edge cases (captured in proptest regressions)
    // with the underlying libraries.
    if line.euclidean_distance(point) < 0.000_001 {
        return Some(*point);
    }

    // If either point is not a "valid" float, bail.
    if !is_valid_float(point.x()) || !is_valid_float(point.y()) {
        return None;
    }

    // TODO: Use haversine_closest_point once a new release is cut which doesn't panic on intersections
    match line.closest_point(point) {
        Closest::Intersection(snapped) | Closest::SinglePoint(snapped) => {
            let (x, y) = (snapped.x(), snapped.y());
            if is_valid_float(x) && is_valid_float(y) {
                Some(snapped)
            } else {
                None
            }
        }
        Closest::Indeterminate => None,
    }
}

/// Calculates the distance a point is from a line (route segment), in meters.
///
/// This function should return a value for valid inputs,
/// but due to the vagaries of floating point numbers
/// (infinity and `NaN` being possible inputs),
/// we return an optional to insulate callers from edge cases.
///
/// # Example
///
/// ```
/// // Diagonal line from the origin to (1,1)
/// use geo::{coord, LineString, point};
/// use ferrostar::algorithms::deviation_from_line;
///
/// let linestring = LineString::new(vec![coord! {x: 0.0, y: 0.0}, coord! {x: 1.0, y: 1.0}]);
///
/// let origin = point! {
///     x: 0.0,
///     y: 0.0,
/// };
/// let midpoint = point! {
///     x: 0.5,
///     y: 0.5,
/// };
/// let off_line = point! {
///     x: 1.0,
/// y: 0.5,
/// };
///
/// // The origin is directly on the line
/// assert_eq!(deviation_from_line(&origin, &linestring), Some(0.0));
///
/// // The midpoint is also directly on the line
/// assert_eq!(deviation_from_line(&midpoint, &linestring), Some(0.0));
///
/// // This point, however is off the line.
/// // That's a huge number, because we're dealing with degrees ;)
/// assert!(deviation_from_line(&off_line, &linestring)
///     .map_or(false, |deviation| deviation - 39312.21257675703 < f64::EPSILON));
/// ```
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
/// NOTE: The [`UserLocation`] should *not* be snapped.
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
                    f64::from(distance),
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
                        f64::from(distance),
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

/// Computes the distance that a point lies along a linestring,
/// assuming that units are latitude and longitude for the geometries.
///
/// The result is given in meters.
/// The result may be [`None`] in case of invalid input such as infinite floats.
fn distance_along(point: &Point, linestring: &LineString) -> Option<f64> {
    let total_length = linestring.haversine_length();
    if total_length == 0.0 {
        return Some(0.0);
    }

    let (_, _, traversed) = linestring.lines().try_fold(
        (0f64, f64::INFINITY, 06f64),
        |(cum_length, closest_dist_to_point, traversed), segment| {
            // Convert to a LineString so we get haversine ops
            let segment_linestring = LineString::from(segment);

            // Compute distance to the line (sadly Euclidean only; no haversine_distance in GeoRust
            // but this is probably OK for now)
            let segment_distance_to_point = segment.euclidean_distance(point);
            // Compute total segment length in meters
            let segment_length = segment_linestring.haversine_length();

            if segment_distance_to_point < closest_dist_to_point {
                let segment_fraction = segment.line_locate_point(point)?;
                Some((
                    cum_length + segment_length,
                    segment_distance_to_point,
                    cum_length + segment_fraction * segment_length,
                ))
            } else {
                Some((
                    cum_length + segment_length,
                    closest_dist_to_point,
                    traversed,
                ))
            }
        },
    )?;
    Some(traversed)
}

/// Computes the distance between a location and the end of the current route step.
/// We assume that input location is pre-snapped to route step's linestring.
///
/// The result may be [`None`] in case of invalid input such as infinite floats.
fn distance_to_end_of_step(
    snapped_location: &Point,
    current_step_linestring: &LineString,
) -> Option<f64> {
    let step_length = current_step_linestring.haversine_length();
    distance_along(snapped_location, current_step_linestring)
        .map(|traversed| step_length - traversed)
}

/// Computes the user's progress along the current trip (distance to destination, ETA, etc.).
///
/// NOTE to callers: `remaining_steps` includes the current step!
pub fn calculate_trip_progress(
    snapped_location: &Point,
    current_step_linestring: &LineString,
    remaining_steps: &[RouteStep],
) -> TripProgress {
    let Some(current_step) = remaining_steps.first() else {
        return TripProgress {
            distance_to_next_maneuver: 0.0,
            distance_remaining: 0.0,
            duration_remaining: 0.0,
        };
    };

    // Calculate the distance and duration till the end of the current route step.
    let distance_to_next_maneuver =
        distance_to_end_of_step(snapped_location, current_step_linestring)
            .unwrap_or(current_step.distance);

    // This could be improved with live traffic data along the route.
    // TODO: Figure out the best way to enable this use case
    let pct_remaining_current_step = if current_step.distance > 0f64 {
        distance_to_next_maneuver / current_step.distance
    } else {
        0f64
    };

    // Get the percentage of duration remaining in the current step.
    let duration_to_next_maneuver = pct_remaining_current_step * current_step.duration;

    // Exit early if there is only the current step:
    if remaining_steps.len() == 1 {
        return TripProgress {
            distance_to_next_maneuver,
            distance_remaining: distance_to_next_maneuver,
            duration_remaining: duration_to_next_maneuver,
        };
    }

    let steps_after_current = &remaining_steps[1..];
    let distance_remaining = distance_to_next_maneuver
        + steps_after_current
            .iter()
            .map(|step| step.distance)
            .sum::<f64>();

    let duration_remaining = duration_to_next_maneuver
        + steps_after_current
            .iter()
            .map(|step| step.duration)
            .sum::<f64>();

    TripProgress {
        distance_to_next_maneuver,
        distance_remaining,
        duration_remaining,
    }
}

/// Convert a vector of geographic coordinates to a [`LineString`].
pub(crate) fn get_linestring(geometry: &[GeographicCoordinate]) -> LineString {
    geometry
        .iter()
        .map(|coord| Coord {
            x: coord.lng,
            y: coord.lat,
        })
        .collect()
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

            prop_assert!(is_valid_float(x) || (!is_valid_float(x1) && x == x1));
            prop_assert!(is_valid_float(y) || (!is_valid_float(y1) && y == y1));

            prop_assert!(line.euclidean_distance(&snapped) < 0.000001);
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
                speed: None
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
            speed: None
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

    #[test]
    fn test_end_of_step_progress(
        x1 in -180f64..180f64, y1 in -90f64..90f64,
        x2 in -180f64..180f64, y2 in -90f64..90f64,
    ) {
        let current_route_step = gen_dummy_route_step(x1, y1, x2, y2);
        let linestring = current_route_step.get_linestring();
        let end = linestring.points().last().expect("Expected at least one point");
        let progress = calculate_trip_progress(&end, &linestring, &[current_route_step]);

        prop_assert_eq!(progress.distance_to_next_maneuver, 0f64);
        prop_assert_eq!(progress.distance_remaining, 0f64);
        prop_assert_eq!(progress.duration_remaining, 0f64);
    }

    #[test]
    fn test_end_of_trip_progress_valhalla_arrival(
        x1: f64, y1: f64,
    ) {
        // This may look wrong, but it's actually how Valhalla (and presumably others)
        // represent a point geometry for the arrival step.
        let current_route_step = gen_dummy_route_step(x1, y1, x1, y1);
        let linestring = current_route_step.get_linestring();
        let end = linestring.points().last().expect("Expected at least one point");
        let progress = calculate_trip_progress(&end, &linestring, &[current_route_step]);

        prop_assert_eq!(progress.distance_to_next_maneuver, 0f64);
        prop_assert_eq!(progress.distance_remaining, 0f64);
        prop_assert_eq!(progress.duration_remaining, 0f64);
    }
}

#[cfg(test)]
mod geom_index_tests {

    use super::*;

    fn gen_line_string() -> LineString<f64> {
        LineString::new(vec![
            coord!(x: 0.0, y: 0.0),
            coord!(x: 1.0, y: 1.0),
            coord!(x: 2.0, y: 2.0),
            coord!(x: 3.0, y: 3.0),
            coord!(x: 4.0, y: 4.0),
        ])
    }

    fn make_user_location(lng: f64, lat: f64) -> UserLocation {
        UserLocation {
            coordinates: GeographicCoordinate { lng, lat },
            horizontal_accuracy: 0.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None,
        }
    }

    #[test]
    fn test_geometry_index_initial() {
        let location = make_user_location(1.1, 1.1);
        let line = gen_line_string();

        let index = index_of_closest_origin_point(location, &line, 0);
        assert_eq!(index, 1);
    }

    #[test]
    fn test_geometry_index_secondary() {
        let location = make_user_location(1.1, 1.1);
        let line = gen_line_string();

        let index = index_of_closest_origin_point(location, &line, 1);
        assert_eq!(index, 1);
    }

    #[test]
    fn test_geometry_index_behind_skip() {
        let location = make_user_location(1.1, 1.1);
        let line = gen_line_string();

        let index = index_of_closest_origin_point(location, &line, 2);
        assert_eq!(index, 2);
    }
}

// TODO: Other unit tests
// - Under and over distance accuracy thresholds
// - Equator and extreme latitude
