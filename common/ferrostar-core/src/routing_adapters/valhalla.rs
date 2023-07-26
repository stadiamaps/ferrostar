use super::{RouteRequest, RoutingRequestGenerationError};
use crate::routing_adapters::RouteRequestGenerator;
use crate::{GeographicCoordinates, UserLocation};
use serde_json::{json, Value as JsonValue};
use std::collections::HashMap;

/// A request generator for Valhalla routing backends operating over HTTP.
///
/// Implementation notes:
/// - The data is requested in OSRM format, as this lends itself well to navigation use cases.
/// - All waypoints are interpreted as [`break`s](https://valhalla.github.io/valhalla/api/turn-by-turn/api-reference/#locations).
#[derive(Debug)]
pub struct ValhallaHttpRequestGenerator {
    /// The full URL of the Valhalla endpoint to access. This will normally be the route endpoint,
    /// but the optimized route endpoint should be interchangeable.
    ///
    /// Users *may* include a query string with an API key.
    endpoint_url: String,
    profile: String,
    // TODO: more tunable parameters; a dict that gets inserted at a bare minimum
}

impl ValhallaHttpRequestGenerator {
    pub fn new(endpoint_url: String, profile: String) -> Self {
        Self {
            endpoint_url,
            profile,
        }
    }
}

impl RouteRequestGenerator for ValhallaHttpRequestGenerator {
    fn generate_request(
        &self,
        user_location: UserLocation,
        waypoints: Vec<GeographicCoordinates>,
    ) -> Result<RouteRequest, RoutingRequestGenerationError> {
        if waypoints.is_empty() {
            Err(RoutingRequestGenerationError::NotEnoughWaypoints)
        } else {
            let headers =
                HashMap::from([("Content-Type".to_string(), "application/json".to_string())]);
            let mut start = json!({
                "lat": user_location.coordinates.lat,
                "lon": user_location.coordinates.lng,
                "street_side_tolerance": core::cmp::max(5, user_location.horizontal_accuracy as u16),
            });
            if let Some(course) = user_location.course_over_ground {
                start["heading"] = course.degrees.into();
                start["heading_tolerance"] = course.accuracy.into();
            }

            let locations: Vec<JsonValue> = std::iter::once(start)
                .chain(waypoints.iter().map(|waypoint| {
                    json!({
                        "lat": waypoint.lat,
                        "lon": waypoint.lng,
                    })
                }))
                .collect();
            // TODO: Figure out if we can use PBF?
            // TODO: Trace attributes as we go rather than pulling a fat payload upfront that we might ditch later?
            let args = json!({
                "format": "osrm",
                "filters": {
                    "action": "include",
                    "attributes": [
                      "shape_attributes.speed",
                      "shape_attributes.speed_limit",
                      "shape_attributes.time",
                      "shape_attributes.length"
                    ]
                },
                "costing": &self.profile,
                "locations": locations,
            });
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
    use crate::CourseOverGround;
    use assert_json_diff::assert_json_include;
    use serde_json::{from_slice, json};
    use std::time::SystemTime;

    const ENDPOINT_URL: &str = "https://api.stadiamaps.com/route/v1";
    const COSTING: &str = "bicycle";
    const USER_LOCATION: UserLocation = UserLocation {
        coordinates: GeographicCoordinates { lat: 0.0, lng: 0.0 },
        horizontal_accuracy: 6.0,
        course_over_ground: None,
        timestamp: SystemTime::UNIX_EPOCH,
    };
    const USER_LOCATION_WITH_COURSE: UserLocation = UserLocation {
        coordinates: GeographicCoordinates { lat: 0.0, lng: 0.0 },
        horizontal_accuracy: 6.0,
        course_over_ground: Some(CourseOverGround {
            degrees: 42,
            accuracy: 12,
        }),
        timestamp: SystemTime::UNIX_EPOCH,
    };
    const WAYPOINTS: [GeographicCoordinates; 2] = [
        GeographicCoordinates { lat: 0.0, lng: 1.0 },
        GeographicCoordinates { lat: 2.0, lng: 3.0 },
    ];

    #[test]
    fn test_not_enough_locations() {
        let generator =
            ValhallaHttpRequestGenerator::new(ENDPOINT_URL.to_string(), COSTING.to_string());

        // At least two locations are required
        assert!(matches!(
            generator.generate_request(USER_LOCATION, Vec::new()),
            Err(RoutingRequestGenerationError::NotEnoughWaypoints)
        ));
    }

    #[test]
    fn test_request_body_without_course() {
        let generator =
            ValhallaHttpRequestGenerator::new(ENDPOINT_URL.to_string(), COSTING.to_string());

        let RouteRequest::HttpPost {
            url: request_url,
            headers,
            body,
        } = generator
            .generate_request(USER_LOCATION, WAYPOINTS.to_vec())
            .unwrap();

        assert_eq!(ENDPOINT_URL, request_url);
        assert_eq!(headers["Content-Type"], "application/json".to_string());

        let body_json: JsonValue = from_slice(&body).expect("Failed to parse request body as JSON");

        assert_json_include!(
            actual: body_json,
            expected: json!({
                "costing": COSTING,
                "locations": [
                    {
                        "lat": 0.0,
                        "lon": 0.0,
                        "street_side_tolerance": 6,
                    },
                    {
                        "lat": 0.0,
                        "lon": 1.0
                    },
                    {
                        "lat": 2.0,
                        "lon": 3.0,
                    }
                ]
            })
        );
    }

    #[test]
    fn test_request_body_with_course() {
        let generator =
            ValhallaHttpRequestGenerator::new(ENDPOINT_URL.to_string(), COSTING.to_string());

        let RouteRequest::HttpPost {
            url: request_url,
            headers,
            body,
        } = generator
            .generate_request(USER_LOCATION_WITH_COURSE, WAYPOINTS.to_vec())
            .unwrap();

        assert_eq!(ENDPOINT_URL, request_url);
        assert_eq!(headers["Content-Type"], "application/json".to_string());

        let body_json: JsonValue = from_slice(&body).expect("Failed to parse request body as JSON");

        assert_json_include!(
            actual: body_json,
            expected: json!({
                "costing": COSTING,
                "locations": [
                    {
                        "lat": 0.0,
                        "lon": 0.0,
                        "street_side_tolerance": 6,
                        "heading": 42,
                        "heading_tolerance": 12,
                    },
                    {
                        "lat": 0.0,
                        "lon": 1.0
                    },
                    {
                        "lat": 2.0,
                        "lon": 3.0,
                    }
                ]
            })
        );
    }

    #[test]
    fn test_request_body_with_invalid_horizontal_accuracy() {
        let generator =
            ValhallaHttpRequestGenerator::new(ENDPOINT_URL.to_string(), COSTING.to_string());
        let location = UserLocation {
            coordinates: GeographicCoordinates { lat: 0.0, lng: 0.0 },
            horizontal_accuracy: -6.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
        };

        let RouteRequest::HttpPost {
            url: request_url,
            headers,
            body,
        } = generator
            .generate_request(location, WAYPOINTS.to_vec())
            .unwrap();

        assert_eq!(ENDPOINT_URL, request_url);
        assert_eq!(headers["Content-Type"], "application/json".to_string());

        let body_json: JsonValue = from_slice(&body).expect("Failed to parse request body as JSON");

        assert_json_include!(
            actual: body_json,
            expected: json!({
                "costing": COSTING,
                "locations": [
                    {
                        "lat": 0.0,
                        "lon": 0.0,
                        "street_side_tolerance": 5,
                    },
                    {
                        "lat": 0.0,
                        "lon": 1.0
                    },
                    {
                        "lat": 2.0,
                        "lon": 3.0,
                    }
                ]
            })
        );
    }
}
