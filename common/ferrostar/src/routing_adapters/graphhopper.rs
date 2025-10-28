//! High-level HTTP request generation for GraphHopper-based HTTP APIs.

use crate::models::{UserLocation, Waypoint};

use crate::routing_adapters::error::{InstantiationError, RoutingRequestGenerationError};
use crate::routing_adapters::{RouteRequest, RouteRequestGenerator};

use serde::{Deserialize, Serialize};
use serde_json::{Map, Value as JsonValue, json};
#[cfg(feature = "std")]
use std::collections::HashMap;
#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

#[derive(Debug, Copy, Clone, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
#[serde(rename_all = "lowercase")]
pub enum GraphHopperVoiceUnits {
    Metric,
    Imperial,
}

/// A route request generator for GraphHopper backends operating over HTTP.
///
/// ## [`WaypointKind`](crate::models::WaypointKind)
///
/// The waypoint kind field of [`Waypoint`] is not currently supported in GraphHopper,
/// as there is no multi-leg support.
/// However, you can configure whether U-turns are allowed with the `pass_through` parameter.
/// Refer to the GraphHopper documentation: <https://docs.graphhopper.com/openapi/routing/getroute>.
///
/// ## Waypoint properties
///
/// The [`Waypoint`] `properties` field is ignored by this route request generator.
///
/// # Examples
///
/// ```
/// use serde_json::{json, Map, Value};
/// use ferrostar::routing_adapters::graphhopper::{GraphHopperHttpRequestGenerator, GraphHopperVoiceUnits};
/// let options: Map<String, Value> = json!({
///     "ch.disable": true,
///     "custom_model": {
///         "distance_influence": 15,
///         "speed": [
///             {
///                 "if": "road_class == MOTORWAY",
///                 "limit_to": "100"
///             }
///         ]
///     }
/// }).as_object().unwrap().to_owned();
/// let request_generator = GraphHopperHttpRequestGenerator::new(
///     "https://graphhopper.com/api/1/navigate/?key=YOUR-API-KEY",
///     "car",
///     "en",
///     GraphHopperVoiceUnits::Metric,
///     options
/// );
/// ```
#[derive(Debug)]
pub struct GraphHopperHttpRequestGenerator {
    endpoint_url: String,
    profile: String,
    locale: String,
    voice_units: GraphHopperVoiceUnits,
    options: Map<String, JsonValue>,
}

impl GraphHopperHttpRequestGenerator {
    /// Creates a new GraphHopper request generator given an endpoint URL, a profile name,
    /// and options to include in the request JSON.
    ///
    /// # Examples
    ///
    /// ```
    /// use serde_json::{json, Map, Value};
    /// use ferrostar::routing_adapters::graphhopper::{GraphHopperHttpRequestGenerator, GraphHopperVoiceUnits};
    /// let options: Map<String, Value> = json!({
    ///     "ch.disable": true,
    ///     "custom_model": {
    ///         "distance_influence": 15,
    ///         "speed": [
    ///             {
    ///                 "if": "road_class == MOTORWAY",
    ///                 "limit_to": "100"
    ///             }
    ///         ]
    ///     }
    /// }).as_object().unwrap().to_owned();
    /// let request_generator = GraphHopperHttpRequestGenerator::new(
    ///     "https://graphhopper.com/api/1/navigate/?key=YOUR-API-KEY",
    ///     "car",
    ///     "en",
    ///     GraphHopperVoiceUnits::Metric,
    ///     options
    /// );
    /// ```
    pub fn new<U: Into<String>, P: Into<String>, L: Into<String>>(
        endpoint_url: U,
        profile: P,
        locale: L,
        voice_units: GraphHopperVoiceUnits,
        options: Map<String, JsonValue>,
    ) -> Self {
        Self {
            endpoint_url: endpoint_url.into(),
            profile: profile.into(),
            locale: locale.into(),
            voice_units,
            options,
        }
    }

    /// Creates a new GraphHopper request generator given an endpoint URL, a profile name,
    /// and options to include in the request JSON.
    /// Options in this constructor are a JSON fragment representing any
    /// options you want to add along with the request.
    ///
    /// # Examples
    ///
    /// ```
    /// # use ferrostar::routing_adapters::graphhopper::{GraphHopperHttpRequestGenerator, GraphHopperVoiceUnits};
    /// let options = r#"{
    ///     "ch.disable": true,
    ///     "custom_model": {
    ///         "distance_influence": 15,
    ///         "speed": [
    ///             {
    ///                 "if": "road_class == MOTORWAY",
    ///                 "limit_to": "100"
    ///             }
    ///         ]
    ///     }
    /// }"#;
    ///
    /// // Without options
    /// let request_generator = GraphHopperHttpRequestGenerator::with_options_json(
    ///     "https://graphhopper.com/api/1/navigate/?key=YOUR-API-KEY",
    ///     "car",
    ///     "en",
    ///     GraphHopperVoiceUnits::Metric,
    ///     None
    /// );
    ///
    /// // With options
    /// let request_generator = GraphHopperHttpRequestGenerator::with_options_json(
    ///     "https://graphhopper.com/api/1/navigate/?key=YOUR-API-KEY",
    ///     "car",
    ///     "en",
    ///     GraphHopperVoiceUnits::Metric,
    ///     Some(options)
    /// );
    /// ```
    pub fn with_options_json<U: Into<String>, P: Into<String>, L: Into<String>>(
        endpoint_url: U,
        profile: P,
        locale: L,
        voice_units: GraphHopperVoiceUnits,
        options_json: Option<&str>,
    ) -> Result<Self, InstantiationError> {
        let parsed_options = match options_json {
            Some(options) => serde_json::from_str::<JsonValue>(options)?
                .as_object()
                .ok_or(InstantiationError::OptionsJsonParseError)?
                .to_owned(),
            None => Map::new(),
        };
        Ok(Self {
            endpoint_url: endpoint_url.into(),
            profile: profile.into(),
            locale: locale.into(),
            voice_units,
            options: parsed_options,
        })
    }
}

impl RouteRequestGenerator for GraphHopperHttpRequestGenerator {
    fn generate_request(
        &self,
        user_location: UserLocation,
        waypoints: Vec<Waypoint>,
    ) -> Result<RouteRequest, RoutingRequestGenerationError> {
        if waypoints.is_empty() {
            Err(RoutingRequestGenerationError::NotEnoughWaypoints)
        } else {
            let headers =
                HashMap::from([("Content-Type".to_string(), "application/json".to_string())]);

            let mut points: Vec<Vec<f64>> = vec![vec![
                user_location.coordinates.lng,
                user_location.coordinates.lat,
            ]];

            points.extend(
                waypoints
                    .iter()
                    .map(|waypoint| vec![waypoint.coordinate.lng, waypoint.coordinate.lat]),
            );

            let mut args = json!({
                "profile": &self.profile,
                "points": points,
                "locale": self.locale,
                "type": "mapbox",
                "voice_units": self.voice_units,
            });

            for (k, v) in &self.options {
                args[k] = v.clone();
            }

            let body = serde_json::to_vec(&args)?;
            Ok(RouteRequest::HttpPost {
                url: self.endpoint_url.clone(),
                headers,
                body,
            })
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::{GeographicCoordinate, WaypointKind};
    use serde_json::from_slice;

    #[cfg(all(feature = "std", not(feature = "web-time")))]
    use std::time::SystemTime;

    #[cfg(feature = "web-time")]
    use web_time::SystemTime;

    const ENDPOINT_URL: &str = "https://graphhopper.com/api/1/navigate/?key=YOUR-API-KEY";
    const COSTING: &str = "car";
    const LOCALE: &str = "en";
    const VOICE_UNITS: GraphHopperVoiceUnits = GraphHopperVoiceUnits::Metric;

    const USER_LOCATION: UserLocation = UserLocation {
        coordinates: GeographicCoordinate { lat: 0.0, lng: 0.0 },
        horizontal_accuracy: 6.0,
        course_over_ground: None,
        timestamp: SystemTime::UNIX_EPOCH,
        speed: None,
    };
    const WAYPOINTS: [Waypoint; 2] = [
        Waypoint {
            coordinate: GeographicCoordinate { lat: 0.0, lng: 1.0 },
            kind: WaypointKind::Break,
            properties: None,
        },
        Waypoint {
            coordinate: GeographicCoordinate { lat: 2.0, lng: 3.0 },
            kind: WaypointKind::Break,
            properties: None,
        },
    ];

    #[test]
    fn not_enough_locations() {
        let generator = GraphHopperHttpRequestGenerator::new(
            ENDPOINT_URL,
            COSTING,
            LOCALE,
            VOICE_UNITS,
            Map::new(),
        );

        // At least two locations are required
        assert!(matches!(
            generator.generate_request(USER_LOCATION, Vec::new()),
            Err(RoutingRequestGenerationError::NotEnoughWaypoints)
        ));
    }

    fn generate_body(
        user_location: UserLocation,
        waypoints: Vec<Waypoint>,
        options_json: Option<&str>,
    ) -> JsonValue {
        let generator = GraphHopperHttpRequestGenerator::with_options_json(
            ENDPOINT_URL,
            COSTING,
            LOCALE,
            VOICE_UNITS,
            options_json,
        )
        .expect("Unable to create request generator");

        match generator.generate_request(user_location, waypoints) {
            Ok(RouteRequest::HttpPost {
                url: request_url,
                headers,
                body,
            }) => {
                assert_eq!(ENDPOINT_URL, request_url);
                assert_eq!(headers["Content-Type"], "application/json".to_string());
                from_slice(&body).expect("Failed to parse request body as JSON")
            }
            Ok(RouteRequest::HttpGet { .. }) => unreachable!(
                "The GraphHopper HTTP request generator currently only generates POST requests"
            ),
            Err(e) => {
                println!("Failed to generate request: {:?}", e);
                json!(null)
            }
        }
    }

    #[test]
    fn request_body_without_options() {
        insta::assert_json_snapshot!(generate_body(USER_LOCATION, WAYPOINTS.to_vec(), None))
    }

    #[test]
    fn request_body_with_custom_profile() {
        insta::assert_json_snapshot!(generate_body(
            USER_LOCATION,
            WAYPOINTS.to_vec(),
            Some(
                r#"{
                    "ch.disable": true,
                    "custom_model": {
                        "distance_influence": 15,
                        "speed": [
                            {
                                "if": "road_class == MOTORWAY",
                                "limit_to": "100"
                            }
                        ]
                    }
                }"#
            )
        ))
    }
}
