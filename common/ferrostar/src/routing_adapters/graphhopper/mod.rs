pub(crate) mod models;

use super::RouteResponseParser;
use polyline::decode_polyline;
use std::f64::consts::PI;

use crate::models::{
    BoundingBox, GeographicCoordinate, RouteStep, Waypoint, UserLocation, ManeuverModifier, ManeuverType,
    VisualInstructionContent, VisualInstruction,
};

use crate::routing_adapters::error::{RoutingRequestGenerationError, InstantiationError, ParsingError};
use crate::routing_adapters::{RouteRequest, Route, RouteRequestGenerator};
use crate::routing_adapters::graphhopper::models::{GraphHopperRouteResponse, GraphHopperPath, DetailEntryValue, MaxSpeedEntry};

#[cfg(all(not(feature = "std"), feature = "alloc"))]
use alloc::collections::BTreeMap as HashMap;
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

            let mut points: Vec<Vec<f64>> = vec![
                vec![user_location.coordinates.lng, user_location.coordinates.lat],
            ];

            points.extend(waypoints.iter().map(|waypoint| {
                vec![waypoint.coordinate.lng, waypoint.coordinate.lat]
            }));

            let mut args = json!({
                // "points_encoded": false,
                "elevation": false, // this lets us use the in-built polyline algo
                "instructions": true,
                "profile": &self.profile,
                "points": points,
                "details": ["leg_time", "max_speed"],
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

pub struct GraphHopperResponseParser {
}

impl GraphHopperResponseParser {
    pub fn new() -> Self {
        Self { }
    }
}

impl RouteResponseParser for GraphHopperResponseParser {
    fn parse_response(&self, response: Vec<u8>) -> Result<Vec<Route>, ParsingError> {
        let res: GraphHopperRouteResponse = serde_json::from_slice(&response)?;

        if let Some(message) = &res.message {
            Err(ParsingError::InvalidStatusCode { code: message.to_string() })
        } else {
            res.paths
                .iter()
                .map(|path| Route::from_graphhopper(path))
                .collect::<Result<Vec<_>, _>>()
        }
    }
}

impl Route {
    pub fn from_graphhopper(path: &GraphHopperPath) -> Result<Self, ParsingError> {

        if !path.points_encoded {
            return Err(ParsingError::InvalidRouteObject{
                error: "points must be encoded".to_string(),
            });
        }

        let linestring = decode_polyline(&path.points, path.points_encoded_multiplier.log(10.0).round() as u32).map_err(|error| {
            ParsingError::InvalidGeometry {
                error: error.to_string(),
            }
        })?;

        let geometry: Vec<GeographicCoordinate> = linestring
                .coords()
                .map(|coord| GeographicCoordinate::from(*coord))
                .collect();

        let speed_limits = path.details.get("max_speed");
        let mut sl_result = Vec::new();
        if let Some(speed_limits) = speed_limits {
            for speed_limit_detail in speed_limits {
               let sub_geo: Vec<GeographicCoordinate> = geometry[speed_limit_detail.start_index..speed_limit_detail.end_index].to_vec();
               let value: Option<f64>;
               match speed_limit_detail.value {
                   Some(DetailEntryValue::Float(f)) => { value = Some(f); },
                   Some(DetailEntryValue::Int(i)) => { value = Some(i as f64); },
                   _ => { value = None; }
               }

               sl_result.push(MaxSpeedEntry {
                  geometry: sub_geo,
                  speed_limit: value,
                  unit: "km/h".to_string(),
               })
            }
        }

        let mut steps = Vec::new();
        for instruction in &path.instructions {
            let turn_angle = if instruction.sign == 6 {
                instruction.turn_angle.map(|angle| ((angle * 180.0 / PI) % 360.0).round() as u16)
            } else {
                None
            };

            let (maneuver_type, maneuver_modifier) = Self::get_maneuver(instruction.sign);
            let visual_instruction_content = VisualInstructionContent {
                 text: instruction.text.clone(), // displayed on the map
                 exit_numbers: instruction.exit_number.into_iter().map(|num| num.to_string()).collect(),
                 maneuver_modifier: maneuver_modifier,
                 maneuver_type: Some(maneuver_type),
                 lane_info: None,
                 roundabout_exit_degrees: turn_angle,
            };
            let visual_instruction = VisualInstruction {
                primary_content: visual_instruction_content,
                secondary_content: None,
                sub_content: None,
                trigger_distance_before_maneuver: instruction.distance,
            };
            let sub_geo: Vec<GeographicCoordinate> = geometry[instruction.interval[0]..instruction.interval[1]].to_vec();
            steps.push(RouteStep {
             geometry: sub_geo,
             distance: instruction.distance,
             duration: instruction.time / 1000.0,
             road_name: Some(instruction.street_name.clone()),
             exits: [].to_vec(),
             annotations: None,
             instruction: instruction.text.clone(), // purpose of this text?
             visual_instructions: [visual_instruction].to_vec(),
             spoken_instructions: [].to_vec(),
             incidents: [].to_vec(),
            });
        }

        let sw = GeographicCoordinate { lng: path.bbox[0], lat: path.bbox[1] };
        let ne = GeographicCoordinate { lng: path.bbox[2], lat: path.bbox[3] };
        Ok(Route {
            geometry,
            bbox: BoundingBox { sw, ne },
            distance: path.distance,
            waypoints: [].to_vec(),
            steps,
        })
    }

    fn get_maneuver(sign: i32) -> (ManeuverType, Option<ManeuverModifier>) {
        // unclear how to specify left/right u-turn or a via point
        match sign {
            -98 => (ManeuverType::Turn, Some(ManeuverModifier::UTurn)), // unknown u-turn
            -8 => (ManeuverType::Turn, Some(ManeuverModifier::UTurn)), // left u-turn
            -7 => (ManeuverType::Turn, Some(ManeuverModifier::Left)),
            -6 => (ManeuverType::ExitRoundabout, None),                // not yet filled from GraphHopper
            -3 => (ManeuverType::Turn, Some(ManeuverModifier::SharpLeft)),
            -2 => (ManeuverType::Turn, Some(ManeuverModifier::Left)),
            -1 => (ManeuverType::Turn, Some(ManeuverModifier::SlightLeft)),
            0 => (ManeuverType::Continue, None),
            1 => (ManeuverType::Turn, Some(ManeuverModifier::SlightRight)),
            2 => (ManeuverType::Turn, Some(ManeuverModifier::Right)),
            3 => (ManeuverType::Turn, Some(ManeuverModifier::SharpRight)),
            4 => (ManeuverType::Arrive, None), // Finish instruction
            5 => (ManeuverType::Depart, None), // Instruction before a via point
            6 => (ManeuverType::Roundabout, None), // Instruction before entering a roundabout
            7 => (ManeuverType::Turn, Some(ManeuverModifier::Right)),
            8 => (ManeuverType::Turn, Some(ManeuverModifier::UTurn)), // right u-turn
            _ => (ManeuverType::Notification, None), // (unknown sign)
        }
    }
}
