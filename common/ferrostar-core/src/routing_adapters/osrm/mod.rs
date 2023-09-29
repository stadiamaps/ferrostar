pub(crate) mod models;

use super::RouteResponseParser;
use crate::routing_adapters::{osrm::models::RouteResponse, Route, RoutingResponseParseError};
use crate::RoutingResponseParseError::ParseError;
use crate::{GeographicCoordinates, RouteStep};
use polyline::decode_polyline;

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
    fn parse_response(&self, response: Vec<u8>) -> Result<Vec<Route>, RoutingResponseParseError> {
        let res: RouteResponse = serde_json::from_slice(&response)?;
        let waypoints: Vec<_> = res
            .waypoints
            .iter()
            .map(|waypoint| GeographicCoordinates {
                lat: waypoint.location.latitude(),
                lng: waypoint.location.longitude(),
            })
            .collect();

        // This isn't the most functional in style, but it's a bit difficult to construct a pipeline
        // today. Stabilization of try_collect may help.
        let mut routes = vec![];
        for route in res.routes {
            let geometry = decode_polyline(&route.geometry, self.polyline_precision)
                .map_err(|error| RoutingResponseParseError::ParseError {
                    error: error.clone(),
                })?
                .coords()
                .map(|coord| GeographicCoordinates::from(*coord))
                .collect();

            let mut steps = vec![];
            for leg in route.legs {
                for step in leg.steps {
                    steps.push(RouteStep::from_osrm(&step, self.polyline_precision)?);
                }
            }

            routes.push(Route {
                geometry,
                waypoints: waypoints.clone(),
                steps,
            })
        }

        Ok(routes)
    }
}

impl RouteStep {
    fn from_osrm(
        value: &models::RouteStep,
        polyline_precision: u32,
    ) -> Result<Self, RoutingResponseParseError> {
        let start_location = decode_polyline(&value.geometry, polyline_precision)
            .map_err(|error| RoutingResponseParseError::ParseError { error })?
            .coords()
            .map(|coord| GeographicCoordinates::from(*coord))
            .take(1)
            .next()
            .ok_or(ParseError {
                error: "No coordinates in geometry".to_string(),
            })?;
        Ok(RouteStep {
            start_location,
            distance: value.distance,
            road_name: value.name.clone(),
            instruction: value.maneuver.get_instruction(),
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const STANDARD_OSRM_POLYLINE6_RESPONSE: &str = r#"{"code":"Ok","routes":[{"geometry":"qikdcB{~dpXmxRbaBuqAoqKyy@svFwNcfKzsAysMdr@evD`m@qrAohBi}A{OkdGjg@ajDZww@lJ}Jrs@}`CvzBq`E`PiB`~A|l@z@feA","legs":[{"steps":[],"summary":"","weight":263.1,"duration":260.2,"distance":1886.3},{"steps":[],"summary":"","weight":370.5,"duration":370.5,"distance":2845.5}],"weight_name":"routability","weight":633.6,"duration":630.7,"distance":4731.8}],"waypoints":[{"hint":"Dv8JgCp3moUXAAAABQAAAAAAAAAgAAAAIXRPQYXNK0AAAAAAcPePQQsAAAADAAAAAAAAABAAAAA6-wAA_kvMAKlYIQM8TMwArVghAwAA7wrXLH_K","distance":4.231521214,"name":"Friedrichstraße","location":[13.388798,52.517033]},{"hint":"JEvdgVmFiocGAAAACgAAAAAAAAB3AAAAppONQOodwkAAAAAA8TeEQgYAAAAKAAAAAAAAAHcAAAA6-wAAfm7MABiJIQOCbswA_4ghAwAAXwXXLH_K","distance":2.795148358,"name":"Torstraße","location":[13.39763,52.529432]},{"hint":"oSkYgP___38fAAAAUQAAACYAAAAeAAAAeosKQlNOX0IQ7CZCjsMGQh8AAABRAAAAJgAAAB4AAAA6-wAASufMAOdwIQNL58wA03AhAwQAvxDXLH_K","distance":2.226580806,"name":"Platz der Vereinten Nationen","location":[13.428554,52.523239]}]}"#;
    const VALHALLA_OSRM_RESPONSE: &str = r#"{"routes":[{"weight_name":"auto","weight":56.002,"duration":11.488,"distance":284,"legs":[{"via_waypoints":[],"annotation":{"maxspeed":[{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"},{"speed":89,"unit":"km/h"}],"speed":[24.7,24.7,24.7,24.7,24.7,24.7,24.7,24.7,24.7],"distance":[23.6,14.9,9.6,13.2,25,28.1,38.1,41.6,90],"duration":[0.956,0.603,0.387,0.535,1.011,1.135,1.539,1.683,3.641]},"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":56.002,"duration":11.488,"steps":[{"intersections":[{"bearings":[288],"entry":[true],"admin_index":0,"out":0,"geometry_index":0,"location":[-149.543469,60.534716]}],"speedLimitUnit":"mph","maneuver":{"type":"depart","instruction":"Drive west on AK 1/Seward Highway.","bearing_after":288,"bearing_before":0,"location":[-149.543469,60.534716]},"speedLimitSign":"mutcd","name":"Seward Highway","duration":11.488,"distance":284,"driving_side":"right","weight":56.002,"mode":"driving","ref":"AK 1","geometry":"wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB"},{"intersections":[{"bearings":[89],"entry":[true],"in":0,"admin_index":0,"geometry_index":9,"location":[-149.548581,60.534991]}],"speedLimitUnit":"mph","maneuver":{"type":"arrive","instruction":"You have arrived at your destination.","bearing_after":0,"bearing_before":269,"location":[-149.548581,60.534991]},"speedLimitSign":"mutcd","name":"Seward Highway","duration":0,"distance":0,"driving_side":"right","weight":0,"mode":"driving","ref":"AK 1","geometry":"}kwmrBhavf|G??"}],"distance":284,"summary":"AK 1"}],"geometry":"wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB"}],"waypoints":[{"distance":0,"name":"AK 1","location":[-149.543469,60.534715]},{"distance":0,"name":"AK 1","location":[-149.548581,60.534991]}],"code":"Ok"}"#;

    #[test]
    fn test_parse_standard_osrm() {
        let parser = OsrmResponseParser::new(6);
        let response = parser
            .parse_response(STANDARD_OSRM_POLYLINE6_RESPONSE.into())
            .expect("Unable to parse OSRM response");
        assert_eq!(response.len(), 1);

        // Verify the geometry
        let expected_coords = vec![
            GeographicCoordinates {
                lat: 52.517033,
                lng: 13.388798,
            },
            GeographicCoordinates {
                lat: 52.527168,
                lng: 13.387228,
            },
            GeographicCoordinates {
                lat: 52.528491,
                lng: 13.393668,
            },
            GeographicCoordinates {
                lat: 52.529432,
                lng: 13.39763,
            },
            GeographicCoordinates {
                lat: 52.529684,
                lng: 13.403888,
            },
            GeographicCoordinates {
                lat: 52.528326,
                lng: 13.411389,
            },
            GeographicCoordinates {
                lat: 52.527507,
                lng: 13.41432,
            },
            GeographicCoordinates {
                lat: 52.52677,
                lng: 13.415657,
            },
            GeographicCoordinates {
                lat: 52.528458,
                lng: 13.417166,
            },
            GeographicCoordinates {
                lat: 52.528728,
                lng: 13.421348,
            },
            GeographicCoordinates {
                lat: 52.528082,
                lng: 13.424085,
            },
            GeographicCoordinates {
                lat: 52.528068,
                lng: 13.424993,
            },
            GeographicCoordinates {
                lat: 52.527885,
                lng: 13.425184,
            },
            GeographicCoordinates {
                lat: 52.527043,
                lng: 13.427263,
            },
            GeographicCoordinates {
                lat: 52.525063,
                lng: 13.43036,
            },
            GeographicCoordinates {
                lat: 52.52479,
                lng: 13.430413,
            },
            GeographicCoordinates {
                lat: 52.523269,
                lng: 13.429678,
            },
            GeographicCoordinates {
                lat: 52.523239,
                lng: 13.428554,
            },
        ];
        assert_eq!(response[0].geometry, expected_coords);
    }

    #[test]
    fn test_parse_valhalla_osrm() {
        let parser = OsrmResponseParser::new(6);
        let response = parser
            .parse_response(VALHALLA_OSRM_RESPONSE.into())
            .expect("Unable to parse Valhalla OSRM response");
        assert_eq!(response.len(), 1);

        // Verify the geometry
        let expected_coords = vec![
            GeographicCoordinates {
                lng: -149.543469,
                lat: 60.534716,
            },
            GeographicCoordinates {
                lng: -149.543879,
                lat: 60.534782,
            },
            GeographicCoordinates {
                lng: -149.544134,
                lat: 60.534829,
            },
            GeographicCoordinates {
                lng: -149.5443,
                lat: 60.534856,
            },
            GeographicCoordinates {
                lng: -149.544533,
                lat: 60.534887,
            },
            GeographicCoordinates {
                lng: -149.544976,
                lat: 60.534941,
            },
            GeographicCoordinates {
                lng: -149.545485,
                lat: 60.534971,
            },
            GeographicCoordinates {
                lng: -149.546177,
                lat: 60.535003,
            },
            GeographicCoordinates {
                lng: -149.546937,
                lat: 60.535008,
            },
            GeographicCoordinates {
                lng: -149.548581,
                lat: 60.534991,
            },
        ];
        assert_eq!(response[0].geometry, expected_coords);
    }
}
