//! Common spatial algorithms which are useful for navigation.

use crate::{
    models::CourseOverGround,
    navigation_controller::models::StepAdvanceStatus::{self, Advanced, EndOfRoute},
};
use crate::{
    models::{GeographicCoordinate, RouteStep, UserLocation},
    navigation_controller::models::TripProgress,
};
use geo::{
    Bearing, Closest, Coord, Distance, Euclidean, Geodesic, Haversine, HaversineClosestPoint,
    Length, LineLocatePoint, LineString, Point,
};

#[cfg(test)]
use {
    crate::navigation_controller::test_helpers::gen_dummy_route_step,
    geo::{coord, point, CoordsIter},
    proptest::{collection::vec, prelude::*},
};

#[cfg(test)]
use crate::test_utils::{arb_coord, arb_user_loc, make_user_location};

/// Get the index of the closest *segment* to the user's location within a [`LineString`].
///
/// A [`LineString`] is a set of points (ex: representing the geometry of a maneuver),
/// and this function identifies which segment a point is closest to,
/// so that you can correctly match attributes along a maneuver.
///
/// In the case of a location being exactly on the boundary
/// (unlikely in the real world, but quite possible in simulations),
/// the *first* segment of equal distance to the location will be matched.
///
/// The maximum value returned is *one less than* the last coordinate index into `line`.
/// Returns [`None`] if `line` contains fewer than two coordinates.
pub fn index_of_closest_segment_origin(location: UserLocation, line: &LineString) -> Option<u64> {
    let point = Point::from(location.coordinates);

    line.lines()
        // Iterate through all segments of the line
        .enumerate()
        // Find the line segment closest to the user's location
        .min_by(|(_, line_segment_1), (_, line_segment_2)| {
            // Note: lines don't implement haversine distances
            // In case you're tempted to say that this looks like cross track distance,
            // note that the Line type here is actually a line *segment*,
            // and we actually want to find the closest segment, not the closest mathematical line.
            let dist1 = Euclidean.distance(line_segment_1, &point);
            let dist2 = Euclidean.distance(line_segment_2, &point);
            dist1.total_cmp(&dist2)
        })
        .map(|(index, _)| index as u64)
}

/// Get the bearing to the next point on the `LineString`.
///
/// Returns [`None`] if the index points at or past the last point in the `LineString`.
fn get_bearing_to_next_point(
    index_along_line: usize,
    line: &LineString,
) -> Option<CourseOverGround> {
    let mut points = line.points().skip(index_along_line);

    let current = points.next()?;
    let next = points.next()?;

    let degrees = Geodesic.bearing(current, next);
    Some(CourseOverGround::new(degrees, None))
}

/// Apply a snapped course to a user location.
///
/// This function snaps the course to travel along the provided line,
/// starting from the given coordinate index along the line.
///
/// If the given index is None or out of bounds, the original location will be returned unmodified.
/// `index_along_line` is optional to improve ergonomics elsewhere in the codebase,
/// despite the API looking a little funny.
pub fn apply_snapped_course(
    location: UserLocation,
    index_along_line: Option<u64>,
    line: &LineString,
) -> UserLocation {
    let snapped_course =
        index_along_line.and_then(|index| get_bearing_to_next_point(index as usize, line));

    let course_over_ground = snapped_course.or(location.course_over_ground);

    UserLocation {
        course_over_ground,
        ..location
    }
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
    if Euclidean.distance(line, point) < 0.000_001 {
        return Some(*point);
    }

    // If either point is not a "valid" float, bail.
    if !is_valid_float(point.x()) || !is_valid_float(point.y()) {
        return None;
    }

    match line.haversine_closest_point(point) {
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
///     y: 0.5,
/// };
///
/// // The origin is directly on the line
/// assert_eq!(deviation_from_line(&origin, &linestring), Some(0.0));
///
/// // The midpoint is also directly on the line
/// assert_eq!(deviation_from_line(&midpoint, &linestring), Some(0.0));
///
/// // This point, however is off the line.
/// // That's a huge number, because we're dealing with points jumping by degrees ;)
/// println!("{:?}", deviation_from_line(&off_line, &linestring));
/// assert!(deviation_from_line(&off_line, &linestring)
///     .map_or(false, |deviation| deviation - 39316.14208341989 < f64::EPSILON));
/// ```
pub fn deviation_from_line(point: &Point, line: &LineString) -> Option<f64> {
    snap_point_to_line(point, line).and_then(|snapped| {
        let distance = Haversine.distance(snapped, *point);

        if distance.is_nan() || distance.is_infinite() {
            None
        } else {
            Some(distance)
        }
    })
}

pub(crate) fn is_within_threshold_to_end_of_linestring(
    current_position: &Point,
    current_step_linestring: &LineString,
    threshold: f64,
) -> bool {
    current_step_linestring
        .coords()
        .last()
        .map_or(false, |end_coord| {
            let end_point = Point::from(*end_coord);
            let distance_to_end = Haversine.distance(end_point, *current_position);

            distance_to_end <= threshold
        })
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
    let total_length = Haversine.length(linestring);
    if total_length == 0.0 {
        return Some(0.0);
    }

    let (_, _, traversed) = linestring.lines().try_fold(
        (0f64, f64::INFINITY, 0f64),
        |(cum_length, closest_dist_to_point, traversed), segment| {
            // Convert to a LineString so we get haversine ops
            let segment_linestring = LineString::from(segment);

            // Compute distance to the line (sadly Euclidean only; no haversine_distance in GeoRust
            // but this is probably OK for now)
            let segment_distance_to_point = Euclidean.distance(&segment, point);
            // Compute total segment length in meters
            let segment_length = Haversine.length(&segment_linestring);

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
/// We assume that input location is pre-snapped to route step's linestring,
/// and that travel is proceeding along the route (not straight line distance).
///
/// The result may be [`None`] in case of invalid input such as infinite floats.
fn travel_distance_to_end_of_step(
    snapped_location: &Point,
    current_step_linestring: &LineString,
) -> Option<f64> {
    let step_length = Haversine.length(current_step_linestring);
    distance_along(snapped_location, current_step_linestring)
        .map(|traversed| step_length - traversed)
}

/// Calculates the distance (in meters) between two user locations.
pub(crate) fn distance_between_locations(
    previous_location: &UserLocation,
    current_location: &UserLocation,
) -> f64 {
    let prev_point: Point = previous_location.coordinates.into();
    let current_point: Point = current_location.coordinates.into();
    Haversine.distance(prev_point, current_point)
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
        travel_distance_to_end_of_step(snapped_location, current_step_linestring)
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

            prop_assert!(Euclidean.distance(&line, &snapped) < 0.000001);
        } else {
            // Edge case 1: extremely small differences in values
            let is_miniscule_difference = (x1 - x2).abs() < 0.00000001 || (y1 - y2).abs() < 0.00000001;
            // Edge case 2: Values which are clearly not WGS84 ;)
            let is_non_wgs84 = (x1 - x2).abs() > 180.0 || (y1 - y2).abs() > 90.0;
            prop_assert!(is_miniscule_difference || is_non_wgs84);
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

    #[test]
    fn test_geometry_index_empty_linestring(
        user_loc in arb_user_loc(0.0)
    ) {
        let index = index_of_closest_segment_origin(user_loc, &LineString::new(vec![]));
        prop_assert_eq!(index, None);
    }

    #[test]
    fn test_geometry_index_single_coord_invalid_linestring(
        coord in arb_coord(),
    ) {
        let index = index_of_closest_segment_origin(make_user_location(coord, 0.0), &LineString::new(vec![coord]));
        prop_assert_eq!(index, None);
    }

    #[test]
    fn test_geometry_index_is_some_for_reasonable_linestrings(
        user_coord in arb_coord(),
        coords in vec(arb_coord(), 2..500)
    ) {
        let index = index_of_closest_segment_origin(make_user_location(user_coord, 0.0), &LineString::new(coords));

        // There are at least two points, so we have a valid segment
        prop_assert_ne!(index, None);
    }

    #[test]
    fn test_geometry_index_at_terminal_coord(
        coords in vec(arb_coord(), 2..500)
    ) {
        let last_coord = coords.last().unwrap();
        let coord_len = coords.len();
        let user_location = make_user_location(*last_coord, 0.0);
        let index = index_of_closest_segment_origin(user_location, &LineString::new(coords));

        // There are at least two points, so we have a valid segment
        prop_assert_ne!(index, None);
        let index = index.unwrap();
        // We should never be able to go past the origin of the final pair
        prop_assert!(index < (coord_len - 1) as u64);
    }

    #[test]
    fn test_bearing_fuzz(coords in vec(arb_coord(), 2..500), index in 0usize..1_000usize) {
        let line = LineString::new(coords);
        let result = get_bearing_to_next_point(index, &line);
        if index < line.coords_count() - 1 {
            prop_assert!(result.is_some());
        } else {
            prop_assert!(result.is_none());
        }
    }

    #[test]
    fn test_bearing_end_of_line(coords in vec(arb_coord(), 2..500)) {
        let line = LineString::new(coords);
        prop_assert!(get_bearing_to_next_point(line.coords_count(), &line).is_none());
        prop_assert!(get_bearing_to_next_point(line.coords_count() - 1, &line).is_none());
        prop_assert!(get_bearing_to_next_point(line.coords_count() - 2, &line).is_some());
    }
}

#[cfg(test)]
mod linestring_based_tests {

    use super::*;

    static COORDS: [Coord; 5] = [
        coord!(x: 0.0, y: 0.0),
        coord!(x: 1.0, y: 1.0),
        coord!(x: 2.0, y: 2.0),
        coord!(x: 3.0, y: 3.0),
        coord!(x: 4.0, y: 4.0),
    ];

    #[test]
    fn test_geometry_index_at_point() {
        let line = LineString::new(COORDS.to_vec());

        // Exactly at a point (NB: does not advance until we move *past* the transition point
        // and are closer to the next line segment!)
        let index = index_of_closest_segment_origin(make_user_location(COORDS[2], 2.0), &line);
        assert_eq!(index, Some(1));
    }

    #[test]
    fn test_geometry_index_near_point() {
        let line = LineString::new(COORDS.to_vec());

        // Very close to an origin point
        let index =
            index_of_closest_segment_origin(make_user_location(coord!(x: 1.1, y: 1.1), 0.0), &line);
        assert_eq!(index, Some(1));

        // Very close to the next point, but not yet "passing" to the next segment!
        let index = index_of_closest_segment_origin(
            make_user_location(coord!(x: 1.99, y: 1.99), 0.0),
            &line,
        );
        assert_eq!(index, Some(1));
    }

    #[test]
    fn test_geometry_index_far_from_point() {
        let line = LineString::new(COORDS.to_vec());

        // "Before" the start
        let index = index_of_closest_segment_origin(
            make_user_location(coord!(x: -1.1, y: -1.1), 0.0),
            &line,
        );
        assert_eq!(index, Some(0));

        // "Past" the end (NB: the last index in the list of coords is 4,
        // but we can never advance past n-1)
        let index = index_of_closest_segment_origin(
            make_user_location(coord!(x: 10.0, y: 10.0), 0.0),
            &line,
        );
        assert_eq!(index, Some(3));
    }
}

#[cfg(test)]
mod bearing_snapping_tests {

    use super::*;

    static COORDS: [Coord; 6] = [
        coord!(x: 0.0, y: 0.0),
        coord!(x: 1.0, y: 1.0),
        coord!(x: 2.0, y: 1.0),
        coord!(x: 2.0, y: 2.0),
        coord!(x: 2.0, y: 1.0),
        coord!(x: 1.0, y: 1.0),
    ];

    #[test]
    fn test_bearing_to_next_point() {
        let line = LineString::new(COORDS.to_vec());

        let bearing = get_bearing_to_next_point(0, &line);
        assert_eq!(
            bearing,
            Some(CourseOverGround {
                degrees: 45,
                accuracy: None
            })
        );

        let bearing = get_bearing_to_next_point(1, &line);
        assert_eq!(
            bearing,
            Some(CourseOverGround {
                degrees: 90,
                accuracy: None
            })
        );

        let bearing = get_bearing_to_next_point(2, &line);
        assert_eq!(
            bearing,
            Some(CourseOverGround {
                degrees: 0,
                accuracy: None
            })
        );

        let bearing = get_bearing_to_next_point(3, &line);
        assert_eq!(
            bearing,
            Some(CourseOverGround {
                degrees: 180,
                accuracy: None
            })
        );

        let bearing = get_bearing_to_next_point(4, &line);
        assert_eq!(
            bearing,
            Some(CourseOverGround {
                degrees: 270,
                accuracy: None
            })
        );

        // At the end
        let bearing = get_bearing_to_next_point(5, &line);
        assert_eq!(bearing, None);
    }

    #[test]
    fn test_apply_snapped_course() {
        let line = LineString::new(COORDS.to_vec());

        // The value of the coordinates does not actually matter;
        // we are testing the course snapping
        let user_location = make_user_location(coord!(x: 5.0, y: 1.0), 0.0);

        // Apply a course to a user location
        let updated_location = apply_snapped_course(user_location, Some(1), &line);

        assert_eq!(
            updated_location.course_over_ground,
            Some(CourseOverGround {
                degrees: 90,
                accuracy: None
            })
        );
    }
}

// TODO: Other unit tests
// - Under and over distance accuracy thresholds
// - Equator and extreme latitude
