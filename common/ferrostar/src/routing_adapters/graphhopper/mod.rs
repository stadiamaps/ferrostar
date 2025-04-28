use crate::models::{UserLocation, Waypoint};

use crate::routing_adapters::error::{InstantiationError, RoutingRequestGenerationError};
use crate::routing_adapters::{RouteRequest, RouteRequestGenerator};

use serde_json::{json, Map, Value as JsonValue};
#[cfg(feature = "std")]
use std::collections::HashMap;

#[derive(Debug)]
pub struct GraphHopperHttpRequestGenerator {
    endpoint_url: String,
    profile: String,
    options: Map<String, JsonValue>,
}

impl GraphHopperHttpRequestGenerator {
    pub fn new(endpoint_url: String, profile: String, options: Map<String, JsonValue>) -> Self {
        Self {
            endpoint_url,
            profile,
            options,
        }
    }

    pub fn with_options_json(
        endpoint_url: String,
        profile: String,
        options_json: Option<&str>,
    ) -> Result<Self, InstantiationError> {
        let parsed_options = match options_json {
            // TODO: Another error variant
            Some(options) => serde_json::from_str::<JsonValue>(options)?
                .as_object()
                .ok_or(InstantiationError::OptionsJsonParseError)?
                .to_owned(),
            None => Map::new(),
        };
        Ok(Self {
            endpoint_url,
            profile,
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
                "locale": "en",
                "type": "mapbox",
                "voice_units": "metric",
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
