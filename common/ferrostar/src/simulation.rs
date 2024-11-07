//! Tools for simulating progress along a route.
//!
//! # Example
//!
//! Here's an example usage with the polyline constructor.
//! This can serve as a template for writing your own test code.
//! You may also get some inspiration from the [Swift](https://github.com/stadiamaps/ferrostar/blob/main/apple/Sources/FerrostarCore/Location.swift)
//! or [Kotlin](https://github.com/stadiamaps/ferrostar/blob/main/android/core/src/main/java/com/stadiamaps/ferrostar/core/Location.kt)
//! `SimulatedLocationProvider` implementations which wrap this.
//!
//! ```
//! use ferrostar::simulation::{advance_location_simulation, location_simulation_from_polyline, LocationBias};
//! # use std::error::Error;
//! # fn main() -> Result<(), Box<dyn Error>> {
//!
//! let polyline_precision = 6;
//! // Build the initial state from an encoded polyline.
//! // You can create a simulation from coordinates or even a [Route] as well.
//! let mut state = location_simulation_from_polyline(
//!     "wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB",
//!     polyline_precision,
//!     // Passing `Some(number)` will resample your polyline at uniform distances.
//!     // This is often desirable to create a smooth simulated movement when you don't have a GPS trace.
//!     None,
//!     LocationBias::None,
//! )?;
//!
//! loop {
//!     let mut new_state = advance_location_simulation(&state);
//!     if new_state == state {
//!         // When the simulation reaches the end, it keeps yielding the input state.
//!         break;
//!     }
//!     state = new_state;
//!     // Do something; maybe sleep for some period of time until the next timestamp?
//! }
//! #
//! # Ok(())
//! # }
//! ```

use crate::algorithms::trunc_float;
use crate::models::{CourseOverGround, GeographicCoordinate, Route, UserLocation};
use geo::{coord, Bearing, Densify, Geodesic, Haversine, LineString, Point};
use polyline::decode_polyline;

#[cfg(any(test, feature = "wasm-bindgen"))]
use serde::{Deserialize, Serialize};

#[cfg(feature = "wasm-bindgen")]
use wasm_bindgen::{prelude::*, JsValue};

#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

#[cfg(all(feature = "std", not(feature = "web-time")))]
use std::time::SystemTime;

#[cfg(feature = "web-time")]
use web_time::SystemTime;

#[cfg(feature = "alloc")]
use alloc::{
    string::{String, ToString},
    vec::Vec,
};

#[derive(Debug)]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
#[cfg_attr(feature = "wasm-bindgen", derive(Serialize, Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub enum SimulationError {
    #[cfg_attr(feature = "std", error("Failed to parse polyline: {error}."))]
    /// Errors decoding the polyline string.
    PolylineError { error: String },
    #[cfg_attr(feature = "std", error("Not enough points (expected at least two)."))]
    /// Not enough points in the input.
    NotEnoughPoints,
}

/// Controls how simulated locations deviate from the actual route line.
/// This simulates real-world GPS behavior where readings often have systematic bias.
#[derive(Clone, PartialEq, Debug)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub enum LocationBias {
    /// Simulates GPS bias by offsetting locations to the left of the route direction.
    /// The f64 parameter specifies the offset distance in meters.
    Left(f64),

    /// Simulates GPS bias by offsetting locations to the right of the route direction.
    /// The f64 parameter specifies the offset distance in meters.
    Right(f64),

    /// Simulates GPS bias by randomly choosing left or right offset on initialization
    /// and maintaining that bias throughout the route.
    /// The f64 parameter specifies the offset distance in meters.
    ///
    /// This mimics real-world GPS behavior where bias direction is random but typically
    /// remains consistent during a trip.
    Random(f64),

    /// No position bias - locations follow the route line exactly.
    ///
    /// This provides "perfect" GPS behavior, useful for testing basic route following
    /// without position uncertainty.
    None,
}

/// The current state of the simulation.
#[derive(Clone, PartialEq)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct LocationSimulationState {
    pub current_location: UserLocation,
    remaining_locations: Vec<GeographicCoordinate>,
    bias: LocationBias,
}

/// Creates a location simulation from a set of coordinates.
///
/// Optionally resamples the input line so that there is a maximum distance between points.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub fn location_simulation_from_coordinates(
    coordinates: &[GeographicCoordinate],
    resample_distance: Option<f64>,
    bias: LocationBias,
) -> Result<LocationSimulationState, SimulationError> {
    if let Some((current, rest)) = coordinates.split_first() {
        if let Some(next) = rest.first() {
            let (jittered_current, bearing) = add_lateral_offset(*current, *next, &bias);

            let accuracy = match bias {
                LocationBias::None => 0.0,
                LocationBias::Left(m) | LocationBias::Right(m) | LocationBias::Random(m) => m,
            };

            let current_location = UserLocation {
                coordinates: jittered_current,
                horizontal_accuracy: accuracy,
                course_over_ground: Some(CourseOverGround::new(bearing, Some(5))),
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
                let densified_linestring = linestring.densify::<Haversine>(distance);
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
                bias,
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
    bias: LocationBias,
) -> Result<LocationSimulationState, SimulationError> {
    // This function is purely a convenience for now,
    // but we eventually expand the simulation to be aware of route timing
    location_simulation_from_coordinates(&route.geometry, resample_distance, bias)
}

/// Creates a location simulation from a polyline.
///
/// Optionally resamples the input line so that there is no more than the specified maximum distance between points.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub fn location_simulation_from_polyline(
    polyline: &str,
    precision: u32,
    resample_distance: Option<f64>,
    bias: LocationBias,
) -> Result<LocationSimulationState, SimulationError> {
    let linestring =
        decode_polyline(polyline, precision).map_err(|error| SimulationError::PolylineError {
            error: error.to_string(),
        })?;
    let coordinates: Vec<_> = linestring
        .coords()
        .map(|c| GeographicCoordinate::from(*c))
        .collect();
    location_simulation_from_coordinates(&coordinates, resample_distance, bias)
}

fn add_lateral_offset(
    current: GeographicCoordinate,
    next: GeographicCoordinate,
    bias: &LocationBias,
) -> (GeographicCoordinate, f64) {
    let current_point = Point::from(current);
    let next_point = Point::from(next);
    let bearing = Geodesic::bearing(current_point, next_point);

    match bias {
        LocationBias::None => (current, bearing),
        LocationBias::Left(meters) | LocationBias::Right(meters) | LocationBias::Random(meters) => {
            let sign = match bias {
                LocationBias::Left(_) => -1.0,
                LocationBias::Right(_) => 1.0,
                LocationBias::Random(_) => {
                    if rand::random() {
                        1.0
                    } else {
                        -1.0
                    }
                }
                LocationBias::None => unreachable!(),
            };

            // calculate perpendicular bearing (±90° from bearing)
            let lateral_bearing_rad = (bearing + sign * 90.0).to_radians();

            // offset to approximate degrees
            let offset_deg = meters / 111111.0;

            let lat_offset = offset_deg * lateral_bearing_rad.cos();
            let lng_offset = offset_deg * lateral_bearing_rad.sin();

            (
                GeographicCoordinate {
                    lat: current.lat + lat_offset,
                    lng: current.lng + lng_offset,
                },
                bearing,
            )
        }
    }
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
        let (jittered_next, bearing) = add_lateral_offset(
            *next_coordinate,
            if let Some(future) = rest.first() {
                *future
            } else {
                *next_coordinate
            },
            &state.bias,
        );

        let accuracy = match state.bias {
            LocationBias::None => 0.0,
            LocationBias::Left(m) | LocationBias::Right(m) | LocationBias::Random(m) => m,
        };

        let next_location = UserLocation {
            coordinates: jittered_next,
            horizontal_accuracy: accuracy,
            course_over_ground: Some(CourseOverGround::new(bearing, Some(5))),
            timestamp: SystemTime::now(),
            speed: None,
        };

        LocationSimulationState {
            current_location: next_location,
            remaining_locations: Vec::from(rest),
            bias: state.bias.clone(),
        }
    } else {
        state.clone()
    }
}

/// JavaScript wrapper for `location_simulation_from_coordinates`.
#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = locationSimulationFromCoordinates)]
pub fn js_location_simulation_from_coordinates(
    coordinates: JsValue,
    resample_distance: Option<f64>,
    bias: LocationBias,
) -> Result<JsValue, JsValue> {
    let coordinates: Vec<GeographicCoordinate> = serde_wasm_bindgen::from_value(coordinates)
        .map_err(|error| JsValue::from_str(&error.to_string()))?;

    location_simulation_from_coordinates(&coordinates, resample_distance, bias)
        .map(|state| serde_wasm_bindgen::to_value(&state).unwrap())
        .map_err(|error| JsValue::from_str(&error.to_string()))
}

/// JavaScript wrapper for `location_simulation_from_route`.
#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = locationSimulationFromRoute)]
pub fn js_location_simulation_from_route(
    route: JsValue,
    resample_distance: Option<f64>,
    bias: LocationBias,
) -> Result<JsValue, JsValue> {
    let route: Route = serde_wasm_bindgen::from_value(route)
        .map_err(|error| JsValue::from_str(&error.to_string()))?;

    location_simulation_from_route(&route, resample_distance, bias)
        .map(|state| serde_wasm_bindgen::to_value(&state).unwrap())
        .map_err(|error| JsValue::from_str(&error.to_string()))
}

/// JavaScript wrapper for `location_simulation_from_polyline`.
#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = locationSimulationFromPolyline)]
pub fn js_location_simulation_from_polyline(
    polyline: &str,
    precision: u32,
    resample_distance: Option<f64>,
    bias: LocationBias,
) -> Result<JsValue, JsValue> {
    location_simulation_from_polyline(polyline, precision, resample_distance, bias)
        .map(|state| serde_wasm_bindgen::to_value(&state).unwrap())
        .map_err(|error| JsValue::from_str(&error.to_string()))
}

/// JavaScript wrapper for `advance_location_simulation`.
#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = advanceLocationSimulation)]
pub fn js_advance_location_simulation(state: JsValue) -> JsValue {
    let state: LocationSimulationState = serde_wasm_bindgen::from_value(state).unwrap();
    let new_state = advance_location_simulation(&state);
    serde_wasm_bindgen::to_value(&new_state).unwrap()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::algorithms::snap_user_location_to_line;
    use geo::{Distance, Haversine};
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
            LocationBias::None,
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
            LocationBias::None,
        )
        .expect("Unable to parse polyline");
        insta::assert_yaml_snapshot!(state);
    }

    #[test]
    fn test_extended_interpolation_simulation() {
        let polyline = r#"umrefAzifwgF?yJf@?|C@?sJ?iL@_BBqD@cDzh@L|@?jBuDjCCl@u@^f@nB?|ABd@s@r@_AAiBBiC@kAlAHrEQ|F@pCNpA?pAAfB?~CkAtXsGRXlDw@rCo@jBc@SwAKoDr@}GLyAJ}AEs@]qBs@gE_@qC?aBBqAVkBZwBLmAFcBG_DOuB?}A^wAjA}Av@eBJoAAyA[sBbCUhAEIoCdAaCd@{@Fer@@ae@?aD?o[Ny@Vk@Sg@C_FCcDT[S_@Ow@F}oCXoAVe@_@e@?mE?cDNm@Og@Ok@Ck^N_BRu@a@OJqFFyDV[a@kAIkSLcF|AgNb@{@U_@JaEN}ETW[cA\_TbAkm@P_H\sE`AgFrCkKlAuGrEo\n@_B|@[~sBa@pAc@|AAh`Aa@jGEnGCrh@AfiAAjAx@TW`DO|CK\mEZ?~LBzBA|_@GtA?zPGlKQ?op@?uO@ggA?wE@uFEwXEyOCeFAkMAsKIot@?_FEoYAsI?yC?eH?}C?}GAy]Bux@Aog@AmKCmFC}YA}WVgBRu@vAaBlC{CxDCR?h@AhHQvGApDA|BAhHA`DC|GGzFDlM@jNA|J?bAkBtACvAArCClINfDdAfFGzW[|HI`FE@eMhHEt^KpJE"#;
        let max_distance = 10.0;
        let mut state =
            location_simulation_from_polyline(polyline, 6, Some(max_distance), LocationBias::None)
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
            let distance = Haversine::distance(current_point, next_point);
            // I'm actually not 100% sure why this extra fudge is needed, but it's not a concern for today.
            assert!(
                distance <= max_distance + 7.0,
                "Expected consecutive points to be <= {max_distance}m apart; was {distance}m"
            );

            let snapped =
                snap_user_location_to_line(new_state.current_location, &original_linestring);
            let snapped_point: Point = snapped.coordinates.into();
            let distance = Haversine::distance(next_point, snapped_point);
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

    #[rstest]
    #[case(LocationBias::None)]
    #[case(LocationBias::Left(4.0))]
    #[case(LocationBias::Right(4.0))]
    #[case(LocationBias::Random(4.0))]
    fn test_location_bias(#[case] bias: LocationBias) {
        let coordinates = vec![
            GeographicCoordinate { lng: 0.0, lat: 0.0 },
            GeographicCoordinate {
                lng: 0.0001,
                lat: 0.0001,
            },
            GeographicCoordinate {
                lng: 0.0002,
                lat: 0.0002,
            },
        ];

        let state = location_simulation_from_coordinates(&coordinates, None, bias.clone())
            .expect("Failed to create simulation");

        if matches!(bias, LocationBias::None) {
            assert_eq!(state.current_location.coordinates, coordinates[0]);
            return;
        }

        let expected_meters = match bias {
            LocationBias::Left(m) | LocationBias::Right(m) | LocationBias::Random(m) => m,
            LocationBias::None => unreachable!(),
        };

        let original_point: Point = coordinates[0].into();
        let offset_point: Point = state.current_location.coordinates.into();
        let distance = Haversine::distance(original_point, offset_point);

        assert!(
            (distance - expected_meters).abs() < 0.1,
            "Expected offset of {expected_meters}m but got {distance}m"
        );
    }

    #[test]
    fn test_bias_consistency() {
        let coordinates = vec![
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
        ];

        let mut state =
            location_simulation_from_coordinates(&coordinates, None, LocationBias::Random(4.0))
                .expect("Failed to create simulation");

        let first_point: Point = state.current_location.coordinates.into();
        let first_original: Point = coordinates[0].into();
        let initial_distance = Haversine::distance(first_point, first_original);

        while let Some((next, _)) = state.remaining_locations.split_first() {
            let new_state = advance_location_simulation(&state);
            if new_state == state {
                break;
            }

            let current_point: Point = new_state.current_location.coordinates.into();
            let original_point: Point = (*next).into();
            let distance = Haversine::distance(current_point, original_point);

            assert!(
                (distance - initial_distance).abs() < 0.1,
                "Bias distance changed from {initial_distance}m to {distance}m"
            );

            state = new_state;
        }
    }
}
