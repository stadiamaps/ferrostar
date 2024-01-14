use crate::models::{GeographicCoordinate, Route};
use polyline::decode_polyline;

#[cfg(test)]
use serde::Serialize;

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum SimulationError {
    #[error("Failed to parse polyline: {error}.")]
    PolylineError { error: String },
    #[error("Not enough points (expected at least two).")]
    NotEnoughPoints,
}

#[derive(uniffi::Record, Clone)]
#[cfg_attr(test, derive(Serialize))]
pub struct LocationSimulationState {
    pub current_location: GeographicCoordinate,
    remaining_locations: Vec<GeographicCoordinate>,
}

#[derive(uniffi::Enum)]
pub enum SimulationSpeed {
    /// Jumps directly to the next location without any interpolation
    JumpToNextLocation,
}

#[uniffi::export]
pub fn location_simulation_from_coordinates(
    coordinates: Vec<GeographicCoordinate>,
) -> Result<LocationSimulationState, SimulationError> {
    if let Some((current_location, rest)) = coordinates.split_first() {
        if rest.is_empty() {
            Err(SimulationError::NotEnoughPoints)
        } else {
            Ok(LocationSimulationState {
                current_location: *current_location,
                remaining_locations: Vec::from(rest),
            })
        }
    } else {
        Err(SimulationError::NotEnoughPoints)
    }
}

#[uniffi::export]
pub fn location_simulation_from_route(
    route: &Route,
) -> Result<LocationSimulationState, SimulationError> {
    // This function is purely a convenience for now,
    // but we eventually expand the simulation to be aware of route timing
    location_simulation_from_coordinates(route.geometry.clone())
}

#[uniffi::export]
pub fn location_simulation_from_polyline(
    polyline: String,
    precision: u32,
) -> Result<LocationSimulationState, SimulationError> {
    let linestring =
        decode_polyline(&polyline, precision).map_err(|error| SimulationError::PolylineError {
            error: error.clone(),
        })?;
    let coordinates: Vec<_> = linestring
        .coords()
        .map(|c| GeographicCoordinate::from(*c))
        .collect();
    location_simulation_from_coordinates(coordinates)
}

/// Returns the next simulation state based on the desired strategy.
/// Results of this can be thought of like a stream from a generator function.
///
/// This function is intended to be called once/second.
/// However, the caller may vary speed to purposefully replay at a faster rate
/// (ex: calling 3x per second will be a triple speed simulation).
///
/// When there are now more locations to visit, returns the same state forever.
#[uniffi::export]
pub fn advance_location_simulation(
    state: &LocationSimulationState,
    speed: SimulationSpeed,
) -> LocationSimulationState {
    if let Some((first, rest)) = state.remaining_locations.split_first() {
        match speed {
            SimulationSpeed::JumpToNextLocation => LocationSimulationState {
                current_location: *first,
                remaining_locations: Vec::from(rest),
            },
        }
    } else {
        state.clone()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn advance_to_next_location() {
        let mut state = location_simulation_from_coordinates(vec![
            GeographicCoordinate { lng: 0.0, lat: 0.0 },
            GeographicCoordinate { lng: 1.0, lat: 1.0 },
            GeographicCoordinate { lng: 2.0, lat: 2.0 },
            GeographicCoordinate { lng: 3.0, lat: 3.0 },
        ]).expect("Unable to initialize simulation");

        let mut states = vec![state.clone()];
        for _ in 0..4 {
            state = advance_location_simulation(&state, SimulationSpeed::JumpToNextLocation);
            states.push(state.clone());
        }

        insta::assert_yaml_snapshot!(states);
    }

    #[test]
    fn state_from_polyline() {
        let state = location_simulation_from_polyline(
            "wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB".to_string(),
            6,
        )
        .expect("Unable to parse polyline");
        insta::assert_yaml_snapshot!(state);
    }
}
