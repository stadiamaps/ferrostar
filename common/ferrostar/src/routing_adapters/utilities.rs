use polyline::decode_polyline;

use crate::models::GeographicCoordinate;

use super::error::ParsingError;

/// Parse a polyline-encoded geometry string into a list of geographic coordinates.
/// If the polyline cannot be decoded, a [`ParsingError`] results.
pub fn get_coordinates_from_geometry(
    geometry: &str,
    polyline_precision: u32,
) -> Result<Vec<GeographicCoordinate>, ParsingError> {
    let linestring = decode_polyline(geometry, polyline_precision).map_err(|error| {
        ParsingError::InvalidGeometry {
            error: error.to_string(),
        }
    })?;

    // TODO: Trait for this common pattern?
    let linestring = linestring
        .coords()
        .map(|coord| GeographicCoordinate::from(*coord))
        .collect();

    Ok(linestring)
}
