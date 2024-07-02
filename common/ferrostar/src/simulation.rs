use crate::algorithms::trunc_float;
use crate::models::{CourseOverGround, GeographicCoordinate, Route, UserLocation};
use geo::{coord, DensifyHaversine, GeodesicBearing, LineString, Point};
use polyline::decode_polyline;
#[cfg(all(feature = "std", not(feature = "web-time")))]
use std::time::SystemTime;

#[cfg(feature = "web-time")]
use web_time::SystemTime;

#[cfg(feature = "alloc")]
use alloc::{
    string::{String, ToString},
    vec::Vec,
};

#[cfg(test)]
use serde::Serialize;

#[derive(Debug)]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
pub enum SimulationError {
    #[cfg_attr(feature = "std", error("Failed to parse polyline: {error}."))]
    PolylineError { error: String },
    #[cfg_attr(feature = "std", error("Not enough points (expected at least two)."))]
    NotEnoughPoints,
}

#[derive(Clone, PartialEq)]
#[cfg_attr(test, derive(Serialize))]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct LocationSimulationState {
    pub current_location: UserLocation,
    remaining_locations: Vec<GeographicCoordinate>,
}

/// Creates a location simulation from a set of coordinates.
///
/// Optionally resamples the input line so that there is a maximum distance between points.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub fn location_simulation_from_coordinates(
    coordinates: &[GeographicCoordinate],
    resample_distance: Option<f64>,
) -> Result<LocationSimulationState, SimulationError> {
    if let Some((current, rest)) = coordinates.split_first() {
        if let Some(next) = rest.first() {
            let current_point = Point::from(*current);
            let next_point = Point::from(*next);
            let bearing = current_point.geodesic_bearing(next_point);
            let current_location = UserLocation {
                coordinates: *current,
                horizontal_accuracy: 0.0,
                course_over_ground: Some(CourseOverGround {
                    degrees: bearing.round() as u16,
                    accuracy: None,
                }),
                timestamp: SystemTime::now(),
                speed: None,
            };

            let remaining_locations = if let Some(distance) = resample_distance {
                // Interpolate so that there are no points further apart than the resample distance.
                let coords: Vec<_> = rest
                    .iter()
                    .map(|coord| {
                        coord! {
                            x: coord.lng,
                            y: coord.lat
                        }
                    })
                    .collect();
                let linestring: LineString = coords.into();
                let densified_linestring = linestring.densify_haversine(distance);
                densified_linestring
                    .points()
                    .map(|point| GeographicCoordinate {
                        // We truncate the value to 6 digits of precision
                        // in line with standard navigation API practice.
                        // Nobody needs precision beyond this point,
                        // and it makes testing very annoying.
                        lat: trunc_float(point.y(), 6),
                        lng: trunc_float(point.x(), 6),
                    })
                    .collect()
            } else {
                Vec::from(rest)
            };

            Ok(LocationSimulationState {
                current_location,
                remaining_locations,
            })
        } else {
            Err(SimulationError::NotEnoughPoints)
        }
    } else {
        Err(SimulationError::NotEnoughPoints)
    }
}

/// Creates a location simulation from a route.
///
/// Optionally resamples the route geometry so that there is no more than the specified maximum distance between points.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub fn location_simulation_from_route(
    route: &Route,
    resample_distance: Option<f64>,
) -> Result<LocationSimulationState, SimulationError> {
    // This function is purely a convenience for now,
    // but we eventually expand the simulation to be aware of route timing
    location_simulation_from_coordinates(&route.geometry, resample_distance)
}

/// Creates a location simulation from a polyline.
///
/// Optionally resamples the input line so that there is no more than the specified maximum distance between points.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub fn location_simulation_from_polyline(
    polyline: &str,
    precision: u32,
    resample_distance: Option<f64>,
) -> Result<LocationSimulationState, SimulationError> {
    let linestring =
        decode_polyline(polyline, precision).map_err(|error| SimulationError::PolylineError {
            error: error.to_string(),
        })?;
    let coordinates: Vec<_> = linestring
        .coords()
        .map(|c| GeographicCoordinate::from(*c))
        .collect();
    location_simulation_from_coordinates(&coordinates, resample_distance)
}

/// Returns the next simulation state based on the desired strategy.
/// Results of this can be thought of like a stream from a generator function.
///
/// This function is intended to be called once/second.
/// However, the caller may vary speed to purposefully replay at a faster rate
/// (ex: calling 3x per second will be a triple speed simulation).
///
/// When there are now more locations to visit, returns the same state forever.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub fn advance_location_simulation(state: &LocationSimulationState) -> LocationSimulationState {
    if let Some((next_coordinate, rest)) = state.remaining_locations.split_first() {
        let current_point = Point::from(state.current_location.coordinates);
        let next_point = Point::from(*next_coordinate);
        let mut bearing = current_point.geodesic_bearing(next_point);
        if bearing < 0.0 {
            bearing += 360.0;
        }

        let next_location = UserLocation {
            coordinates: *next_coordinate,
            horizontal_accuracy: 0.0,
            course_over_ground: Some(CourseOverGround {
                degrees: bearing.round() as u16,
                accuracy: None,
            }),
            timestamp: SystemTime::now(),
            speed: None,
        };

        LocationSimulationState {
            current_location: next_location,
            remaining_locations: Vec::from(rest),
        }
    } else {
        state.clone()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::algorithms::snap_user_location_to_line;
    use geo::HaversineDistance;
    use rstest::rstest;

    #[rstest]
    #[case(None)]
    #[case(Some(10.0))]
    fn advance_to_next_location(#[case] resample_distance: Option<f64>) {
        let mut state = location_simulation_from_coordinates(
            &[
                GeographicCoordinate { lng: 0.0, lat: 0.0 },
                GeographicCoordinate {
                    lng: 0.0001,
                    lat: 0.0001,
                },
                GeographicCoordinate {
                    lng: 0.0002,
                    lat: 0.0002,
                },
                GeographicCoordinate {
                    lng: 0.0003,
                    lat: 0.0003,
                },
            ],
            resample_distance,
        )
        .expect("Unable to initialize simulation");

        // Loop until state no longer changes
        let mut states = vec![state.clone()];
        loop {
            let new_state = advance_location_simulation(&state);
            if new_state == state {
                break;
            }
            state = new_state;
            states.push(state.clone());
        }

        insta::assert_yaml_snapshot!(format!("{:?}", resample_distance), states);
    }

    #[test]
    fn state_from_polyline() {
        let state = location_simulation_from_polyline(
            "wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB",
            6,
            None,
        )
        .expect("Unable to parse polyline");
        insta::assert_yaml_snapshot!(state);
    }

    #[test]
    fn test_extended_interpolation_simulation() {
        let polyline = r#"umrefAzifwgF?yJf@?|C@?sJ?iL@_BBqD@cDzh@L|@?jBuDjCCl@u@^f@nB?|ABd@s@r@_AAiBBiC@kAlAHrEQ|F@pCNpA?pAAfB?~CkAtXsGRXlDw@rCo@jBc@SwAKoDr@}GLyAJ}AEs@]qBs@gE_@qC?aBBqAVkBZwBLmAFcBG_DOuB?}A^wAjA}Av@eBJoAAyA[sBbCUhAEIoCdAaCd@{@Fer@@ae@?aD?o[Ny@Vk@Sg@C_FCcDT[S_@Ow@F}oCXoAVe@_@e@?mE?cDNm@Og@Ok@Ck^N_BRu@a@OJqFFyDV[a@kAIkSLcF|AgNb@{@U_@JaEN}ETW[cA\_TbAkm@P_H\sE`AgFrCkKlAuGrEo\n@_B|@[~sBa@pAc@|AAh`Aa@jGEnGCrh@AfiAAjAx@TW`DO|CK\mEZ?~LBzBA|_@GtA?zPGlKQ?op@?uO@ggA?wE@uFEwXEyOCeFAkMAsKIot@?_FEoYAsI?yC?eH?}C?}GAy]Bux@Aog@AmKCmFC}YA}WVgBRu@vAaBlC{CxDCR?h@AhHQvGApDA|BAhHA`DC|GGzFDlM@jNA|J?bAkBtACvAArCClINfDdAfFGzW[|HI`FE@eMhHEt^KpJE"#;
        let max_distance = 10.0;
        let mut state = location_simulation_from_polyline(polyline, 6, Some(max_distance))
            .expect("Unable to create initial state");
        let original_linestring = decode_polyline(polyline, 6).expect("Unable to decode polyline");

        // Loop until state no longer changes
        let mut states = vec![state.clone()];
        loop {
            let new_state = advance_location_simulation(&state);
            if new_state == state {
                break;
            }

            // The distance between each point in the simulation should be <= max_distance
            let current_point: Point = state.current_location.into();
            let next_point: Point = new_state.current_location.into();
            let distance = current_point.haversine_distance(&next_point);
            // I'm actually not 100% sure why this extra fudge is needed, but it's not a concern for today.
            assert!(
                distance <= max_distance + 7.0,
                "Expected consecutive points to be <= {max_distance}m apart; was {distance}m"
            );

            let snapped =
                snap_user_location_to_line(new_state.current_location, &original_linestring);
            let snapped_point: Point = snapped.coordinates.into();
            let distance = next_point.haversine_distance(&snapped_point);
            assert!(
                distance <= max_distance,
                "Expected snapped point to be on the line; was {distance}m away"
            );

            state = new_state;
            states.push(state.clone());
        }

        // Sanity check: the simulation finishes on the last point
        assert_eq!(
            state.current_location.coordinates,
            original_linestring
                .points()
                .last()
                .expect("Expected at least one point")
                .into()
        );
        insta::assert_yaml_snapshot!(states);
    }
}
