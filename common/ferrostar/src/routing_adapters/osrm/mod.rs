//! Response parsing for OSRM-compatible JSON (including Stadia Maps, Valhalla, Mapbox, etc.).

pub(crate) mod models;
pub mod utilities;

use super::RouteResponseParser;
use crate::models::{
    AnyAnnotationValue, GeographicCoordinate, Incident, LaneInfo, RouteStep, SpokenInstruction,
    VisualInstruction, VisualInstructionContent, Waypoint, WaypointKind,
};
use crate::routing_adapters::utilities::get_coordinates_from_geometry;
use crate::routing_adapters::{
    osrm::models::{
        Route as OsrmRoute, RouteResponse, RouteStep as OsrmRouteStep, Waypoint as OsrmWaypoint,
    },
    ParsingError, Route,
};
#[cfg(all(not(feature = "std"), feature = "alloc"))]
use alloc::{string::ToString, vec, vec::Vec};
use geo::BoundingRect;
use models::BannerContent;
use polyline::decode_polyline;
use utilities::get_annotation_slice;
use uuid::Uuid;

/// A response parser for OSRM-compatible routing backends.
///
/// The parser is NOT limited to only the standard OSRM format; many Valhalla/Mapbox tags are also
/// parsed and are included in the final route.
#[derive(Debug)]
pub struct OsrmResponseParser {
    polyline_precision: u32,
}

impl OsrmResponseParser {
    pub fn new(polyline_precision: u32) -> Self {
        Self { polyline_precision }
    }
}

impl RouteResponseParser for OsrmResponseParser {
    fn parse_response(&self, response: Vec<u8>) -> Result<Vec<Route>, ParsingError> {
        let res: RouteResponse = serde_json::from_slice(&response)?;

        if res.code == "Ok" {
            res.routes
                .iter()
                .map(|route| Route::from_osrm(route, &res.waypoints, self.polyline_precision))
                .collect::<Result<Vec<_>, _>>()
        } else {
            let error_description = match res.message {
                Some(message) => format!("{}: {}", res.code, message),
                None => res.code,
            };
            Err(ParsingError::InvalidStatusCode { code: error_description })
        }
    }
}

impl Route {
    /// Create a route from an OSRM route and OSRM waypoints.
    ///
    /// # Arguments
    /// * `route` - The OSRM route.
    /// * `waypoints` - The OSRM waypoints.
    /// * `polyline_precision` - The precision of the polyline.
    pub fn from_osrm(
        route: &OsrmRoute,
        waypoints: &[OsrmWaypoint],
        polyline_precision: u32,
    ) -> Result<Self, ParsingError> {
        let via_waypoint_indices: Vec<_> = route
            .legs
            .iter()
            .flat_map(|leg| leg.via_waypoints.iter().map(|via| via.waypoint_index))
            .collect();

        let waypoints: Vec<_> = waypoints
            .iter()
            .enumerate()
            .map(|(idx, waypoint)| Waypoint {
                coordinate: GeographicCoordinate {
                    lat: waypoint.location.latitude(),
                    lng: waypoint.location.longitude(),
                },
                kind: if via_waypoint_indices.contains(&idx) {
                    WaypointKind::Via
                } else {
                    WaypointKind::Break
                },
            })
            .collect();

        Self::from_osrm_with_standard_waypoints(route, &waypoints, polyline_precision)
    }

    /// Create a route from an OSRM route and Ferrostar waypoints.
    ///
    /// # Arguments
    /// * `route` - The OSRM route.
    /// * `waypoints` - The Ferrostar waypoints.
    /// * `polyline_precision` - The precision of the polyline.
    pub fn from_osrm_with_standard_waypoints(
        route: &OsrmRoute,
        waypoints: &[Waypoint],
        polyline_precision: u32,
    ) -> Result<Self, ParsingError> {
        let linestring = decode_polyline(&route.geometry, polyline_precision).map_err(|error| {
            ParsingError::InvalidGeometry {
                error: error.to_string(),
            }
        })?;
        if let Some(bbox) = linestring.bounding_rect() {
            let geometry: Vec<GeographicCoordinate> = linestring
                .coords()
                .map(|coord| GeographicCoordinate::from(*coord))
                .collect();

            let steps = route
                .legs
                .iter()
                .flat_map(|leg| {
                    // Converts all single value annotation vectors into a single vector witih a value object.
                    let annotations = leg
                        .annotation
                        .as_ref()
                        .map(|leg_annotation| utilities::zip_annotations(leg_annotation.clone()));

                    // Convert all incidents into a vector of Incident objects.
                    let incident_items = leg
                        .incidents
                        .iter()
                        .map(Incident::from)
                        .collect::<Vec<Incident>>();

                    // Index for the annotations slice
                    let mut start_index: usize = 0;

                    leg.steps.iter().map(move |step| {
                        let step_geometry =
                            get_coordinates_from_geometry(&step.geometry, polyline_precision)?;

                        // Slice the annotations for the current step.
                        // The annotations array represents segments between coordinates.
                        //
                        // 1. Annotations should never repeat.
                        // 2. Each step has one less annotation than coordinate.
                        // 3. The last step never has annotations as it's two of the route's last coordinate (duplicate).
                        let step_index_len = step_geometry.len() - 1_usize;
                        let end_index = start_index + step_index_len;

                        let annotation_slice =
                            get_annotation_slice(annotations.clone(), start_index, end_index).ok();

                        let relevant_incidents_slice = incident_items
                            .iter()
                            .filter(|incident| {
                                let incident_start = incident.geometry_index_start as usize;

                                match incident.geometry_index_end {
                                    Some(end) => {
                                        let incident_end = end as usize;
                                        incident_start >= start_index && incident_end <= end_index
                                    }
                                    None => {
                                        incident_start >= start_index && incident_start <= end_index
                                    }
                                }
                            })
                            .map(|incident| {
                                let mut adjusted_incident = incident.clone();
                                if adjusted_incident.geometry_index_start - start_index as u64 > 0 {
                                    adjusted_incident.geometry_index_start -= start_index as u64;
                                } else {
                                    adjusted_incident.geometry_index_start = 0;
                                }

                                if let Some(end) = adjusted_incident.geometry_index_end {
                                    let adjusted_end = end - start_index as u64;
                                    adjusted_incident.geometry_index_end =
                                        Some(if adjusted_end > end_index as u64 {
                                            end_index as u64
                                        } else {
                                            adjusted_end
                                        });
                                }
                                adjusted_incident
                            })
                            .collect::<Vec<Incident>>();

                        start_index = end_index;

                        RouteStep::from_osrm_and_geom(
                            step,
                            step_geometry,
                            annotation_slice,
                            relevant_incidents_slice,
                        )
                    })
                })
                .collect::<Result<Vec<_>, _>>()?;

            Ok(Route {
                geometry,
                bbox: bbox.into(),
                distance: route.distance,
                waypoints: waypoints.into(),
                steps,
            })
        } else {
            Err(ParsingError::InvalidGeometry {
                error: "Bounding box could not be calculated".to_string(),
            })
        }
    }
}

impl RouteStep {
    fn extract_exit_numbers(banner_content: &BannerContent) -> Vec<String> {
        banner_content
            .components
            .iter()
            .filter(|component| component.component_type.as_deref() == Some("exit-number"))
            .filter_map(|component| component.text.clone())
            .collect()
    }

    fn from_osrm_and_geom(
        value: &OsrmRouteStep,
        geometry: Vec<GeographicCoordinate>,
        annotations: Option<Vec<AnyAnnotationValue>>,
        incidents: Vec<Incident>,
    ) -> Result<Self, ParsingError> {
        let visual_instructions = value
            .banner_instructions
            .iter()
            .map(|banner| VisualInstruction {
                primary_content: VisualInstructionContent {
                    text: banner.primary.text.clone(),
                    maneuver_type: banner.primary.maneuver_type,
                    maneuver_modifier: banner.primary.maneuver_modifier,
                    roundabout_exit_degrees: banner.primary.roundabout_exit_degrees,
                    lane_info: None,
                    exit_numbers: Self::extract_exit_numbers(&banner.primary),
                },
                secondary_content: banner.secondary.as_ref().map(|secondary| {
                    VisualInstructionContent {
                        text: secondary.text.clone(),
                        maneuver_type: secondary.maneuver_type,
                        maneuver_modifier: secondary.maneuver_modifier,
                        roundabout_exit_degrees: banner.primary.roundabout_exit_degrees,
                        lane_info: None,
                        exit_numbers: Self::extract_exit_numbers(&secondary),
                    }
                }),
                sub_content: banner.sub.as_ref().map(|sub| VisualInstructionContent {
                    text: sub.text.clone(),
                    maneuver_type: sub.maneuver_type,
                    maneuver_modifier: sub.maneuver_modifier,
                    roundabout_exit_degrees: sub.roundabout_exit_degrees,
                    lane_info: {
                        let lane_infos: Vec<LaneInfo> = sub
                            .components
                            .iter()
                            .filter(|component| component.component_type.as_deref() == Some("lane"))
                            .map(|component| LaneInfo {
                                active: component.active.unwrap_or(false),
                                directions: component.directions.clone().unwrap_or_default(),
                                active_direction: component.active_direction.clone(),
                            })
                            .collect();

                        if lane_infos.is_empty() {
                            None
                        } else {
                            Some(lane_infos)
                        }
                    },
                    exit_numbers: Self::extract_exit_numbers(&sub),
                }),
                trigger_distance_before_maneuver: banner.distance_along_geometry,
            })
            .collect();

        let spoken_instructions = value
            .voice_instructions
            .iter()
            .map(|instruction| SpokenInstruction {
                text: instruction.announcement.clone(),
                ssml: instruction.ssml_announcement.clone(),
                trigger_distance_before_maneuver: instruction.distance_along_geometry,
                utterance_id: Uuid::new_v4(),
            })
            .collect();

        // Convert the annotations to a vector of json strings.
        // This allows us to safely pass the RouteStep through the FFI boundary.
        // The host platform can then parse an arbitrary annotation object.
        let annotations_as_strings: Option<Vec<String>> = annotations.map(|annotations_vec| {
            annotations_vec
                .iter()
                .map(|annotation| serde_json::to_string(annotation).unwrap())
                .collect()
        });

        let exits = match value.exits.clone() {
            Some(exit_text) => exit_text.split(';').map(|s| s.trim().to_string()).collect(),
            None => Vec::new(),
        };

        Ok(RouteStep {
            geometry,
            // TODO: Investigate using the haversine distance or geodesics to normalize.
            // Valhalla in particular is a bit nonstandard. See https://github.com/valhalla/valhalla/issues/1717
            distance: value.distance,
            duration: value.duration,
            road_name: value.name.clone(),
            exits,
            instruction: value.maneuver.get_instruction(),
            visual_instructions,
            spoken_instructions,
            annotations: annotations_as_strings,
            incidents,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const STANDARD_OSRM_POLYLINE6_RESPONSE: &str =
        include_str!("fixtures/standard_osrm_polyline6_response.json");
    const VALHALLA_OSRM_RESPONSE: &str = include_str!("fixtures/valhalla_osrm_response.json");
    const VALHALLA_OSRM_RESPONSE_VIA_WAYS: &str =
        include_str!("fixtures/valhalla_osrm_response_via_ways.json");
    const VALHALLA_EXTENDED_OSRM_RESPONSE: &str =
        include_str!("fixtures/valhalla_extended_osrm_response.json");
    const VALHALLA_OSRM_RESPONSE_WITH_EXITS: &str =
        include_str!("fixtures/valhalla_osrm_response_with_exit_info.json");

    #[test]
    fn parse_standard_osrm() {
        let parser = OsrmResponseParser::new(6);
        let routes = parser
            .parse_response(STANDARD_OSRM_POLYLINE6_RESPONSE.into())
            .expect("Unable to parse OSRM response");
        insta::assert_yaml_snapshot!(routes);
    }

    #[test]
    fn parse_valhalla_osrm() {
        let parser = OsrmResponseParser::new(6);
        let routes = parser
            .parse_response(VALHALLA_OSRM_RESPONSE.into())
            .expect("Unable to parse Valhalla OSRM response");

        insta::assert_yaml_snapshot!(routes, {
            ".**.annotations" => "redacted annotations json strings vec"
        });
    }

    #[test]
    fn parse_valhalla_osrm_with_via_ways() {
        let parser = OsrmResponseParser::new(6);
        let routes = parser
            .parse_response(VALHALLA_OSRM_RESPONSE_VIA_WAYS.into())
            .expect("Unable to parse Valhalla OSRM response");

        insta::assert_yaml_snapshot!(routes, {
            ".**.annotations" => "redacted annotations json strings vec"
        });
    }

    #[test]
    fn parse_valhalla_asserting_annotation_lengths() {
        let parser = OsrmResponseParser::new(6);
        let routes = parser
            .parse_response(VALHALLA_OSRM_RESPONSE.into())
            .expect("Unable to parse Valhalla OSRM response");

        // Loop through every step and validate that the length of the annotations
        // matches the length of the geometry minus one. This is because each annotation
        // represents a segment between two coordinates.
        for (route_index, route) in routes.iter().enumerate() {
            for (step_index, step) in route.steps.iter().enumerate() {
                if step_index == route.steps.len() - 1 {
                    // The arrival step is 2 of the same coordinates.
                    // So annotations will be None.
                    assert_eq!(step.annotations, None);
                    continue;
                }

                let step = step.clone();
                let annotations = step.annotations.expect("No annotations");
                assert_eq!(
                    annotations.len(),
                    step.geometry.len() - 1,
                    "Route {}, Step {}",
                    route_index,
                    step_index
                );
            }
        }
    }

    #[test]
    fn parse_valhalla_asserting_sub_maneuvers() {
        let parser = OsrmResponseParser::new(6);
        let routes = parser
            .parse_response(VALHALLA_EXTENDED_OSRM_RESPONSE.into())
            .expect("Unable to parse Valhalla Extended OSRM response");

        // Collect all sub_contents into a vector
        let sub_contents: Vec<_> = routes
            .iter()
            .flat_map(|route| &route.steps)
            .filter_map(|step| {
                step.visual_instructions
                    .iter()
                    .find_map(|instruction: &VisualInstruction| instruction.sub_content.as_ref())
            })
            .collect();

        // Assert that there's exactly one sub maneuver instructions as is the case in the test data
        assert_eq!(
            sub_contents.len(),
            1,
            "Expected exactly one sub banner instructions"
        );

        if let Some(sub_content) = sub_contents.first() {
            // Ensure that there are 4 pieces of lane information in the sub banner instructions
            if let Some(lane_info) = &sub_content.lane_info {
                assert_eq!(lane_info.len(), 4);
            } else {
                panic!("Expected lane information, but could not find it");
            }
        } else {
            panic!("No sub banner instructions found in any of the steps")
        }
    }

    #[test]
    fn parse_osrm_with_exits() {
        let parser = OsrmResponseParser::new(6);
        let routes = parser
            .parse_response(VALHALLA_OSRM_RESPONSE_WITH_EXITS.into())
            .expect("Unable to parse OSRM response");

        insta::assert_yaml_snapshot!(routes, {
            ".**.annotations" => "redacted annotations json strings vec"
        });
    }

    #[test]
    fn test_osrm_parser_with_empty_route_array() {
        let error_json = r#"{
            "code": "NoRoute",
            "message": "No route found between the given coordinates",
            "routes": []
        }"#;

        let parser = OsrmResponseParser::new(6);
        let result = parser.parse_response(error_json.as_bytes().to_vec());

        assert!(result.is_err());
        if let Err(ParsingError::InvalidStatusCode { code }) = result {
            assert_eq!(code, "NoRoute: No route found between the given coordinates");
        } else {
            panic!("Expected InvalidStatusCode error with proper message");
        }
    }

    #[test]
    fn test_osrm_parser_with_missing_route_field() {
        let error_json = r#"{
            "code": "NoRoute",
            "message": "No route found between the given coordinates"
        }"#;

        let parser = OsrmResponseParser::new(6);
        let result = parser.parse_response(error_json.as_bytes().to_vec());

        assert!(result.is_err());
        if let Err(ParsingError::InvalidStatusCode { code }) = result {
            assert_eq!(code, "NoRoute: No route found between the given coordinates");
        } else {
            panic!("Expected InvalidStatusCode error with proper message");
        }
    }
}
