use crate::models::{GeographicCoordinates, UserLocation};
use geo::{Closest, HaversineClosestPoint, LineString, Point};

/// Snaps a user location to the closest point on a route line.
pub(crate) fn snap_to_line(location: UserLocation, line: &LineString) -> UserLocation {
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
