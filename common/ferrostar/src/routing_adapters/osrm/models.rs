//! OSRM models from the API spec: <http://project-osrm.org/docs/v5.5.1/api/>
//!
//! Note that in some cases we optionally allow for extensions that have been made to the spec
//! by others which are now pseudo-standardized (ex: Mapbox). We omit some fields which are not
//! needed for navigation.

use crate::models::{ManeuverModifier, ManeuverType};
use alloc::{string::String, vec::Vec};
use serde::Deserialize;
use serde_json::Value;
#[cfg(feature = "alloc")]
use std::collections::HashMap;

#[derive(Deserialize, Debug)]
#[serde(transparent)]
pub struct Coordinate {
    tuple: (f64, f64),
}

impl Coordinate {
    pub fn latitude(&self) -> f64 {
        self.tuple.1
    }

    pub fn longitude(&self) -> f64 {
        self.tuple.0
    }
}

#[derive(Deserialize, Debug)]
pub struct RouteResponse {
    /// The response code.
    ///
    /// Ok indicates success. TODO: enumerate others?
    pub code: String,
    pub routes: Vec<Route>,
    pub waypoints: Vec<Waypoint>,
}

/// A route between two or more waypoints.
#[derive(Deserialize, Debug)]
pub struct Route {
    /// The estimated travel time, in seconds.
    pub duration: f64,
    /// The distance traveled by the route, in meters.
    pub distance: f64,
    /// The geometry of the route.
    ///
    /// NOTE: This library assumes that 1) an overview geometry will always be requested, and
    /// 2) that it will be a polyline (whether it is a polyline5 or polyline6 can be determined
    /// by the [`crate::routing_adapters::RouteResponseParser`]).
    pub geometry: String,
    /// The legs between the given waypoints.
    pub legs: Vec<RouteLeg>,
}

/// A route between exactly two waypoints.
#[derive(Deserialize, Debug)]
pub struct RouteLeg {
    pub annotation: Option<AnyAnnotation>,
    /// The estimated travel time, in seconds.
    pub duration: f64,
    /// The distance traveled this leg, in meters.
    pub distance: f64,
    /// A sequence of steps with turn-by-turn instructions.
    pub steps: Vec<RouteStep>,
    /// A Mapbox and Valhalla extension which indicates which waypoints are passed through rather than creating a new leg.
    #[serde(default)]
    pub via_waypoints: Vec<ViaWaypoint>,
}

#[derive(Deserialize, Debug, Clone, PartialEq)]
pub struct AnyAnnotation {
    #[serde(flatten)]
    pub values: HashMap<String, Vec<Value>>,
}

/// The max speed json units that can be associated with annotations.
/// [MaxSpeed/Units](https://wiki.openstreetmap.org/wiki/Map_Features/Units#Speed)
/// TODO: We could generalize this as or map from this to a `UnitSpeed` enum.
#[derive(Deserialize, PartialEq, Debug, Clone)]
pub enum MaxSpeedUnits {
    #[serde(rename = "km/h")]
    KilometersPerHour,
    #[serde(rename = "mph")]
    MilesPerHour,
    #[serde(rename = "knots")]
    Knots,
}

/// The local posted speed limit between a pair of coordinates.
#[derive(Debug, Clone)]
#[allow(dead_code)] // TODO: https://github.com/stadiamaps/ferrostar/issues/271
pub enum MaxSpeed {
    None { none: bool },
    Unknown { unknown: bool },
    Known { speed: f64, unit: MaxSpeedUnits },
}

#[derive(Deserialize, Debug)]
pub struct RouteStep {
    /// The distance from the start of the current maneuver to the following step, in meters.
    pub distance: f64,
    /// The estimated travel time, in seconds.
    pub duration: f64,
    /// The (unsimplified) geometry of the route segment.
    ///
    /// NOTE: This library assumes that the geometry will always be a polyline.
    pub geometry: String,
    /// The name of the way along which travel proceeds.
    pub name: Option<String>,
    /// A reference number or code for the way (if one is available).
    #[serde(rename = "ref")]
    pub reference: Option<String>,
    /// A pronunciation hint for the name of the way.
    pub pronunciation: Option<String>,
    /// The mode of transportation.
    pub mode: Option<String>,
    /// The maneuver for this step
    pub maneuver: StepManeuver,
    /// TODO: docs
    pub intersections: Vec<Intersections>,

    /// A list of exits (name or number), separated by semicolons.
    ///
    /// NOTE: This annotation is not in the official spec, but is a common extension used by Mapbox
    /// and Valhalla.
    pub exits: Option<String>,

    /// The side of the way on which traffic proceeds.
    ///
    /// NOTE: This annotation is not in the official spec, but is a common extension used by Mapbox
    /// and Valhalla.
    pub driving_side: Option<String>,
    // Mapbox and Valhalla extensions that might be useful later
    // pub rotary_name: Option<String>,
    // pub rotary_pronunciation: Option<String>,
    /// Textual instructions that are displayed as a banner; supported by Mapbox and Valhalla
    #[serde(default, rename = "bannerInstructions")]
    pub banner_instructions: Vec<BannerInstruction>,
    /// Textual instructions that are displayed as a banner; supported by Mapbox and Stadia Maps
    #[serde(default, rename = "voiceInstructions")]
    pub voice_instructions: Vec<VoiceInstruction>,
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct BannerInstruction {
    /// How far (in meters) from the upcoming maneuver the instruction should start being displayed
    pub distance_along_geometry: f64,
    pub primary: BannerContent,
    pub secondary: Option<BannerContent>,
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct VoiceInstruction {
    pub announcement: String,
    pub ssml_announcement: Option<String>,
    /// How far (in meters) from the upcoming maneuver the instruction should be announced
    pub distance_along_geometry: f64,
}

#[derive(Deserialize, Debug)]
pub struct BannerContent {
    pub text: String,
    #[serde(rename = "type")]
    pub maneuver_type: Option<ManeuverType>,
    #[serde(rename = "modifier")]
    pub maneuver_modifier: Option<ManeuverModifier>,
    /// The degree at which the maneuver exits the roundabout.
    /// 180 indicates that the effect of exiting will be to continue in the same direction as
    /// the original travel.
    #[serde(rename = "degrees")]
    pub roundabout_exit_degrees: Option<u16>,
}

#[derive(Deserialize, Debug)]
pub struct StepManeuver {
    /// The coordinate at which the maneuver takes place.
    pub location: Coordinate,
    /// The clockwise angle from true north to the direction of travel immediately *before*
    /// the maneuver.
    pub bearing_before: u16,
    /// The clockwise angle from true north to the direction of travel immediately *after*
    /// the maneuver.
    pub bearing_after: u16,
    /// A string indicating the type of maneuver.
    ///
    /// Note that even though there are `new name` and `notification` instructions, the
    /// `mode` and `name` (of the parent [`RouteStep`]) can change between *any* pair of instructions.
    /// They only offer a fallback in case there is nothing else to report.
    /// TODO: Model this as an enum. Note that new types may be introduced, and anything unknown to the client should be handled like a turn.
    #[serde(rename = "type")]
    pub maneuver_type: String,
    /// An optional string indicating the direction change of the maneuver.
    /// TODO: Model this as an enum.
    pub modifier: Option<String>,
    /// Non-standard extension in Mapbox and Valhalla where the instruction is computed server-side
    instruction: Option<String>,
}

impl StepManeuver {
    // TODO: This is a placeholder implementation.
    // Most commercial offerings offer server-side synthesis of voice instructions.
    // However, we might consider synthesizing these locally too.
    // This will be rather cumbersome with localization though.
    fn synthesize_instruction(&self, _locale: &str) -> String {
        String::from("TODO: OSRM instruction synthesis")
    }

    pub fn get_instruction(&self) -> String {
        self.instruction
            .clone()
            .unwrap_or_else(|| self.synthesize_instruction("en-US"))
    }
}

#[derive(Deserialize, Debug)]
pub struct Intersections {
    /// The location of the intersection
    pub location: Coordinate,
    /// A list of bearing values that are available at the intersection.
    ///
    /// These describe all available roads at the intersection.
    pub bearings: Vec<u16>,
    /// An array of strings signifying the classes (as specified in the profile) of the road
    /// exiting the intersection.
    ///
    /// Note that Valhalla servers do not return this property.
    #[serde(default)]
    pub classes: Vec<String>,
    /// A list of entry flags, corresponding 1:1 to the list of bearings.
    ///
    /// This value indicates whether the respective road could be entered on a valid route (not
    /// violating some restriction).
    pub entry: Vec<bool>,
    /// An index into the bearings/entry array used to calculate the bearing just before the turn.
    ///
    /// The clockwise angle from true north to the direction of travel immediately before the
    /// maneuver/passing the intersection.
    /// To get the bearing in the direction of driving, the bearing has to be rotated by
    /// 180 degrees. Not supplied for `depart` maneuvers.
    #[serde(default)]
    #[serde(rename = "in")]
    pub intersection_in: usize,
    /// An index into the bearings/entry array used to calculate the bearing just before the turn.
    ///
    /// The clockwise angle from true north to the direction of travel immediately after the
    /// maneuver/passing the intersection.  Not supplied for `arrive` maneuvers.
    #[serde(default)]
    #[serde(rename = "out")]
    pub intersection_out: usize,
    /// A list of turn [`Lane`]s available at the intersection (if info is available).
    ///
    /// Lanes are listed in left-to-right order.
    #[serde(default)]
    pub lanes: Vec<Lane>,
}

#[derive(Deserialize, Debug)]
pub struct Lane {
    /// An indication (ex: marking on the road, sign, etc.) for a turn lane.
    ///
    /// A lane may have multiple indications (ex: both straight and left),
    /// TODO: Turn this into an enum
    pub indications: Vec<String>,
    /// Whether the lane is a valid choice for the current maneuver
    pub valid: bool,
    // TODO: Mapbox and Valhalla extensions: `active` and `valid_indication`
}

#[derive(Deserialize, Debug)]
pub struct Waypoint {
    /// The name of the street that the waypoint snapped to.
    pub name: Option<String>,
    /// The distance (in meters) between the snapped point and the input coordinate.
    pub distance: Option<f64>,
    /// The waypoint's location on the road network.
    pub location: Coordinate,
}

#[derive(Deserialize, Debug)]
pub struct ViaWaypoint {
    /// The distance (in meters) from the leg origin
    pub distance_from_start: f64,
    /// The geometry point index of the location (leg-specific).
    pub geometry_index: f64,
    /// The waypoint's index in the array of waypoints.
    pub waypoint_index: usize,
}

#[cfg(test)]
mod tests {
    use super::*;

    // TODO: Route
    // TODO: RouteLeg

    #[test]
    fn deserialize_annotation() {
        // Example from Mapbox's public docs, which include several annotations not supported at
        // the time of this writing.
        let data = r#"{
            "distance": [
                4.294596842089401,
                5.051172053200946,
                5.533254065167979,
                6.576513793849532,
                7.4449640160938015,
                8.468757534990829,
                15.202780313562714,
                7.056346577326572
            ],
            "duration": [
                1,
                1.2,
                2,
                1.6,
                1.8,
                2,
                3.6,
                1.7
            ],
            "speed": [
                4.3,
                4.2,
                2.8,
                4.1,
                4.1,
                4.2,
                4.2,
                4.2
            ],
            "congestion": [
                "low",
                "moderate",
                "moderate",
                "moderate",
                "heavy",
                "heavy",
                "heavy",
                "heavy"
            ],
            "maxspeed": [
              {
                "speed": 56,
                "unit": "km/h"
              },
              {
                "speed": 56,
                "unit": "km/h"
              },
              {
                "speed": 56,
                "unit": "km/h"
              },
              {
                "speed": 56,
                "unit": "km/h"
              },
              {
                "speed": 56,
                "unit": "km/h"
              },
              {
                "speed": 56,
                "unit": "km/h"
              },
              {
                "speed": 56,
                "unit": "km/h"
              },
              {
                "speed": 56,
                "unit": "km/h"
              }
            ]
        }"#;

        let annotation: AnyAnnotation =
            serde_json::from_str(data).expect("Failed to parse Annotation");

        insta::with_settings!({sort_maps => true}, {
            insta::assert_yaml_snapshot!(annotation.values);
        });
    }

    #[test]
    fn deserialize_banner_instruction() {
        // Example from Mapbox's public docs
        let data = r#"
        {
          "distanceAlongGeometry": 100,
          "primary": {
            "type": "turn",
            "modifier": "left",
            "text": "I 495 North / I 95",
            "components": [
              {
                "text": "I 495",
                "imageBaseURL": "https://s3.amazonaws.com/mapbox/shields/v3/i-495",
                "type": "icon"
              },
              {
                "text": "North",
                "type": "text",
                "abbr": "N",
                "abbr_priority": 0
              },
              {
                "text": "/",
                "type": "delimiter"
              },
              {
                "text": "I 95",
                "imageBaseURL": "https://s3.amazonaws.com/mapbox/shields/v3/i-95",
                "type": "icon"
              }
            ]
          },
          "secondary": {
            "type": "turn",
            "modifier": "left",
            "text": "Baltimore / Northern Virginia",
            "components": [
              {
                "text": "Baltimore",
                "type": "text"
              },
              {
                "text": "/",
                "type": "text"
              },
              {
                "text": "Northern Virginia",
                "type": "text"
              }
            ]
          },
          "sub": {
            "text": "",
            "components": [
              {
                "text": "",
                "type": "lane",
                "directions": [
                  "left"
                ],
                "active": true
              },
              {
                "text": "",
                "type": "lane",
                "directions": [
                  "left",
                  "straight"
                ],
                "active": true
              },
              {
                "text": "",
                "type": "lane",
                "directions": [
                  "right"
                ],
                "active": false
              }
            ]
          }
        }
        "#;

        let instruction: BannerInstruction =
            serde_json::from_str(data).expect("Failed to parse Annotation");

        assert_eq!(instruction.distance_along_geometry, 100.0);
        assert_eq!(instruction.primary.text, "I 495 North / I 95");
        assert_eq!(instruction.primary.maneuver_type, Some(ManeuverType::Turn));
        assert_eq!(
            instruction.primary.maneuver_modifier,
            Some(ManeuverModifier::Left)
        );

        let secondary = instruction.secondary.expect("Expected secondary content");
        assert_eq!(secondary.text, "Baltimore / Northern Virginia");
        assert_eq!(secondary.maneuver_type, Some(ManeuverType::Turn));
        assert_eq!(secondary.maneuver_modifier, Some(ManeuverModifier::Left));
    }

    #[test]
    fn test_max_speed_unknown() {
        let max_speed = MaxSpeed::Unknown { unknown: true };
        assert_eq!(max_speed.get_as_meters_per_second(), None);
    }

    #[test]
    fn test_max_speed_kph() {
        let max_speed = MaxSpeed::Known {
            speed: 100.0,
            unit: MaxSpeedUnits::KilometersPerHour,
        };
        assert_eq!(
            max_speed.get_as_meters_per_second(),
            Some(27.778000000000002)
        );
    }

    #[test]
    fn test_max_speed_mph() {
        let max_speed = MaxSpeed::Known {
            speed: 60.0,
            unit: MaxSpeedUnits::MilesPerHour,
        };
        assert_eq!(max_speed.get_as_meters_per_second(), Some(26.8224));
    }
}
