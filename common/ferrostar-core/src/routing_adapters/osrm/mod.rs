pub(crate) mod models;

use super::RouteResponseParser;
use crate::routing_adapters::{osrm::models::RouteResponse, Route, RoutingResponseParseError};
use crate::GeographicCoordinate;
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
        Ok(res.routes.iter().map(|route| {
            let geometry = decode_polyline(&route.geometry, self.polyline_precision)
                .expect("As of v0.10.0, this method appears to be unable to fail based on its body; open an issue upstream.")
                .coords()
                .map(|coord| GeographicCoordinate::from(*coord))
                .collect();

            Route {
                geometry
            }
        }).collect())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const STANDARD_OSRM_POLYLINE6_RESPONSE: &str = r#"{"code":"Ok","routes":[{"geometry":"qikdcB{~dpXmxRbaBuqAoqKyy@svFwNcfKzsAysMdr@evD`m@qrAohBi}A{OkdGjg@ajDZww@lJ}Jrs@}`CvzBq`E`PiB`~A|l@z@feA","legs":[{"steps":[],"summary":"","weight":263.1,"duration":260.2,"distance":1886.3},{"steps":[],"summary":"","weight":370.5,"duration":370.5,"distance":2845.5}],"weight_name":"routability","weight":633.6,"duration":630.7,"distance":4731.8}],"waypoints":[{"hint":"Dv8JgCp3moUXAAAABQAAAAAAAAAgAAAAIXRPQYXNK0AAAAAAcPePQQsAAAADAAAAAAAAABAAAAA6-wAA_kvMAKlYIQM8TMwArVghAwAA7wrXLH_K","distance":4.231521214,"name":"Friedrichstraße","location":[13.388798,52.517033]},{"hint":"JEvdgVmFiocGAAAACgAAAAAAAAB3AAAAppONQOodwkAAAAAA8TeEQgYAAAAKAAAAAAAAAHcAAAA6-wAAfm7MABiJIQOCbswA_4ghAwAAXwXXLH_K","distance":2.795148358,"name":"Torstraße","location":[13.39763,52.529432]},{"hint":"oSkYgP___38fAAAAUQAAACYAAAAeAAAAeosKQlNOX0IQ7CZCjsMGQh8AAABRAAAAJgAAAB4AAAA6-wAASufMAOdwIQNL58wA03AhAwQAvxDXLH_K","distance":2.226580806,"name":"Platz der Vereinten Nationen","location":[13.428554,52.523239]}]}"#;

    #[test]
    fn test_parse_standard_osrm() {
        let parser = OsrmResponseParser::new(6);
        let response = parser
            .parse_response(STANDARD_OSRM_POLYLINE6_RESPONSE.into())
            .expect("Unable to parse OSRM response");
        assert_eq!(response.len(), 1);

        // Verify the geometry
        let expected_coords = vec![
            GeographicCoordinate {
                lat: 52.517033,
                lng: 13.388798,
            },
            GeographicCoordinate {
                lat: 52.527168,
                lng: 13.387228,
            },
            GeographicCoordinate {
                lat: 52.528491,
                lng: 13.393668,
            },
            GeographicCoordinate {
                lat: 52.529432,
                lng: 13.39763,
            },
            GeographicCoordinate {
                lat: 52.529684,
                lng: 13.403888,
            },
            GeographicCoordinate {
                lat: 52.528326,
                lng: 13.411389,
            },
            GeographicCoordinate {
                lat: 52.527507,
                lng: 13.41432,
            },
            GeographicCoordinate {
                lat: 52.52677,
                lng: 13.415657,
            },
            GeographicCoordinate {
                lat: 52.528458,
                lng: 13.417166,
            },
            GeographicCoordinate {
                lat: 52.528728,
                lng: 13.421348,
            },
            GeographicCoordinate {
                lat: 52.528082,
                lng: 13.424085,
            },
            GeographicCoordinate {
                lat: 52.528068,
                lng: 13.424993,
            },
            GeographicCoordinate {
                lat: 52.527885,
                lng: 13.425184,
            },
            GeographicCoordinate {
                lat: 52.527043,
                lng: 13.427263,
            },
            GeographicCoordinate {
                lat: 52.525063,
                lng: 13.43036,
            },
            GeographicCoordinate {
                lat: 52.52479,
                lng: 13.430413,
            },
            GeographicCoordinate {
                lat: 52.523269,
                lng: 13.429678,
            },
            GeographicCoordinate {
                lat: 52.523239,
                lng: 13.428554,
            },
        ];
        assert_eq!(response[0].geometry, expected_coords);
    }
}
