use crate::{GeographicCoordinates, UserLocation};

/// Snaps a user location to the closest point on a route line.
pub(crate) fn snap_to_line(location: UserLocation, line: &[GeographicCoordinates]) -> UserLocation {
    // TODO: Find the nearest point on the route line (probably an existing crate; maybe in GeoRust)
    location
}