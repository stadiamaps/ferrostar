//! High-level HTTP request generation for Valhalla-based HTTP APIs.

use super::{RouteRequest, RoutingRequestGenerationError};
use crate::models::{GeographicCoordinate, UserLocation, Waypoint, WaypointKind};
use crate::routing_adapters::RouteRequestGenerator;
#[cfg(all(not(feature = "std"), feature = "alloc"))]
use alloc::collections::BTreeMap as HashMap;
use serde_json::{json, Map, Value as JsonValue};
#[cfg(feature = "std")]
use std::collections::HashMap;

use crate::routing_adapters::error::InstantiationError;
#[cfg(feature = "alloc")]
use alloc::{
    string::{String, ToString},
    vec::Vec,
};
use serde::{Deserialize, Serialize};
use serde_with::skip_serializing_none;
#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

/// Waypoint properties supported by Valhalla servers.
///
/// Our docstrings are short here, since Valhalla is the final authority.
/// Refer to <https://valhalla.github.io/valhalla/api/turn-by-turn/api-reference/#locations>
/// for more details, including default values.
/// Other Valhalla-based APIs such as Stadia Maps or Mapbox may have slightly different defaults.
/// Refer to your vendor's documentation when in doubt.
///
/// NOTE: Waypoint properties will NOT currently be echoed back in OSRM format,
/// so these are sent to the server one time.
#[derive(Copy, Clone, PartialEq, Debug, Default, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct ValhallaWaypointProperties {
    /// Preferred direction of travel for the start from the location.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub heading: Option<u16>,
    /// How close in degrees a given street's angle must be
    /// in order for it to be considered as in the same direction of the heading parameter.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub heading_tolerance: Option<u16>,
    /// Minimum number of nodes (intersections) reachable for a given edge
    /// (road between intersections) to consider that edge as belonging to a connected region.
    /// Disconnected edges are ignored.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub minimum_reachability: Option<u16>,
    /// The number of meters about this input location within which edges
    /// will be considered as candidates for said location.
    /// If there are no candidates within this distance,
    /// it will return the closest candidate within reason.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub radius: Option<u16>,
    /// Determines whether the location should be visited from the same, opposite or either side of the road,
    /// with respect to the side of the road the given locale drives on.
    ///
    /// NOTE: If the location is not offset from the road centerline
    /// or is very close to an intersection, this option has no effect!
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub preferred_side: Option<ValhallaWaypointPreferredSide>,
    /// Latitude of the map location in degrees.
    ///
    /// If provided, the waypoint location will still be used for routing,
    /// but these coordinates will determine the side of the street.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub display_coordinate: Option<GeographicCoordinate>,
    /// The cutoff at which we will assume the input is too far away from civilization
    /// to be worth correlating to the nearest graph elements.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub search_cutoff: Option<u32>,
    /// During edge correlation, this is the tolerance used to determine whether to snap
    /// to the intersection rather than along the street.
    /// If the snap location is within this distance from the intersection,
    /// the intersection is used instead.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub node_snap_tolerance: Option<u16>,
    /// A tolerance in meters from the edge centerline used for determining the side of the street
    /// that the location is on.
    /// If the distance to the centerline is less than this tolerance,
    /// no side will be inferred.
    /// Otherwise, the left or right side will be selected depending on the direction of travel.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub street_side_tolerance: Option<u16>,
    /// The max distance in meters that the input coordinates or display lat/lon can be
    /// from the edge centerline for them to be used for determining the side of the street.
    /// Beyond this distance, no street side is inferred.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub street_side_max_distance: Option<u16>,
    /// Disables the `preferred_side` when set to `same` or `opposite`
    /// if the edge has a road class less than that provided by `street_side_cutoff`.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub street_side_cutoff: Option<ValhallaRoadClass>,
    /// A set of optional filters to exclude candidate edges based on their attributes.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub search_filter: Option<ValhallaLocationSearchFilter>,
}

impl Waypoint {
    /// A convenience constructor for creating waypoints with Valhalla rich location properties.
    pub fn new_with_valhalla_properties(
        coordinate: GeographicCoordinate,
        kind: WaypointKind,
        properties: ValhallaWaypointProperties,
    ) -> Self {
        Self {
            coordinate,
            kind,
            properties: Some(serde_json::to_vec(&properties).expect("Serialization of Valhalla waypoint properties failed. This is a bug in Ferrostar; please open an issue report on GitHub."))
        }
    }
}

/// A convenience helper for creating waypoints with Valhalla rich location properties.
///
/// Regrettably this must live as a top-level function unless constructors for record types lands
/// in UniFFI:
/// <https://github.com/mozilla/uniffi-rs/issues/1935>.
#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn create_waypoint_with_valhalla_properties(
    coordinate: GeographicCoordinate,
    kind: WaypointKind,
    properties: ValhallaWaypointProperties,
) -> Waypoint {
    Waypoint::new_with_valhalla_properties(coordinate, kind, properties)
}

/// Specifies a preferred side for departing from / arriving at a location.
///
/// Examples:
/// - Germany drives on the right side of the road. A value of `same` will only allow leaving
///   or arriving at a location such that it is on your right.
/// - Australia drives on the left side of the road. Passing a value of `same` will only allow
///   leaving or arriving at a location such that it is on your left.
#[derive(Copy, Clone, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
#[serde(rename_all = "snake_case")]
pub enum ValhallaWaypointPreferredSide {
    /// You must depart from or arrive at the location on the _same_ side as you drive.
    Same,
    /// You must depart from or arrive at the location on the _opposite_ side as you drive.
    Opposite,
    /// No preference; you can depart or arrive from any direction.
    Either,
}

/// A road class in the Valhalla taxonomy.
///
/// These are ordered from highest (fastest travel speed) to lowest.
#[derive(Copy, Clone, PartialEq, PartialOrd, Debug, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
#[serde(rename_all = "snake_case")]
pub enum ValhallaRoadClass {
    Motorway,
    Trunk,
    Primary,
    Secondary,
    Tertiary,
    Unclassified,
    Residential,
    ServiceOther,
}

/// A set of optional filters to exclude candidate edges based on their attributes.
#[skip_serializing_none]
#[derive(Copy, Clone, PartialEq, Debug, Default, Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
#[serde(rename_all = "snake_case")]
pub struct ValhallaLocationSearchFilter {
    /// Whether to exclude roads marked as tunnels.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub exclude_tunnel: Option<bool>,
    /// Whether to exclude roads marked as bridges.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub exclude_bridge: Option<bool>,
    /// Whether to exclude roads with tolls.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub exclude_tolls: Option<bool>,
    /// Whether to exclude ferries.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub exclude_ferry: Option<bool>,
    /// Whether to exclude roads marked as ramps.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub exclude_ramp: Option<bool>,
    /// Whether to exclude roads marked as closed due to a live traffic closure.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub exclude_closures: Option<bool>,
    /// The lowest road class allowed.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub min_road_class: Option<ValhallaRoadClass>,
    /// The highest road class allowed.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub max_road_class: Option<ValhallaRoadClass>,
    /// If specified, will only consider edges that are on or traverse the passed floor level.
    /// It will set `search_cutoff` to a default value of 300 meters if no cutoff value is passed.
    /// Additionally, if a `search_cutoff` is passed, it will be clamped to 1000 meters.
    #[cfg_attr(feature = "uniffi", uniffi(default))]
    pub level: Option<f32>,
}

/// A route request generator for Valhalla backends operating over HTTP.
///
/// # Rich waypoint support
///
/// Valhalla-compatible backends like Stadia Maps support a dozen or so
/// extra waypoint properties which help you specify better routes.
///
/// ## [`WaypointKind`]
///
/// The waypoint kind field of [`Waypoint`] carries the same meaning as the respective
/// [`type` strings in Valhalla API](https://valhalla.github.io/valhalla/api/turn-by-turn/api-reference/#locations).
///
/// ## Waypoint properties
///
/// Additional properties are supported via the [`Waypoint`] `properties` field.
/// To enforce type safety and make it easier to use, we provide the [`ValhallaWaypointProperties`] struct.
/// Internally, `generate_request` will first deserialize `properties` into this.
/// If `properties` are in an invalid format, `generate_request` will fail.
///
/// # Examples
///
/// ```
/// # #[cfg(all(feature = "std", not(feature = "web-time")))]
/// # use std::time::SystemTime;
/// # #[cfg(feature = "web-time")]
/// # use web_time::SystemTime;
/// use serde_json::{json, Map, Value};
/// use ferrostar::models::{GeographicCoordinate, UserLocation, Waypoint, WaypointKind};
/// use crate::ferrostar::routing_adapters::RouteRequestGenerator;
/// use ferrostar::routing_adapters::valhalla::{ValhallaHttpRequestGenerator, ValhallaWaypointPreferredSide, ValhallaWaypointProperties};
/// let options: Map<String, Value> = json!({
///     "costing_options": {
///         "low_speed_vehicle": {
///             "vehicle_type": "golf_cart"
///         }
///     }
/// }).as_object().unwrap().to_owned();;
/// let request_generator = ValhallaHttpRequestGenerator::new("https://api.stadiamaps.com/route/v1?api_key=YOUR-API-KEY", "low_speed_vehicle", options);
///
/// // Generate a request...
///
/// // User location (probably comes from a GPS; this is just test code)
/// let user_location = UserLocation {
///     coordinates: GeographicCoordinate {
///         lng: 0.0,
///         lat: 0.0,
///     },
///     horizontal_accuracy: 0.0,
///     course_over_ground: None,
///     timestamp: SystemTime::now(),
///     speed: None,
/// };
///
/// // Waypoints
/// let waypoints = vec![
///     Waypoint::new_with_valhalla_properties(
///         GeographicCoordinate { lat: 2.0, lng: 3.0 },
///         WaypointKind::Break,
///         ValhallaWaypointProperties {
///             preferred_side: Some(ValhallaWaypointPreferredSide::Same),
///             ..Default::default()
///         },
///     ),
/// ];
///
/// let request = request_generator.generate_request(user_location, waypoints);
/// assert!(request.is_ok(), "If we did everything correctly, we should have gotten a valid request");
/// ```
#[derive(Debug)]
pub struct ValhallaHttpRequestGenerator {
    /// The full URL of the Valhalla endpoint to access. This will normally be the route endpoint,
    /// but the optimized route endpoint should be interchangeable.
    ///
    /// Users *may* include a query string with an API key.
    endpoint_url: String,
    /// The Valhalla costing model to use.
    profile: String,
    // TODO: Language, units, and other top-level parameters
    /// Arbitrary key/value pairs which override the defaults.
    ///
    /// These can contain complex nested structures,
    /// as in the case of `costing_options`.
    options: Map<String, JsonValue>,
}

impl ValhallaHttpRequestGenerator {
    /// Creates a new Valhalla request generator given an endpoint URL, a profile name,
    /// and options to include in the request JSON.
    ///
    /// # Examples
    ///
    /// ```
    /// use serde_json::{json, Map, Value as JsonValue};
    /// # use ferrostar::routing_adapters::valhalla::ValhallaHttpRequestGenerator;
    /// // Example for illustration; you should do proper error checking when parsing this way,
    /// // or else use [`ValhallaHttpRequestGenerator::with_options_json`]
    /// let options: Map<String, JsonValue> = json!({
    ///     "costing_options": {
    ///         "low_speed_vehicle": {
    ///             "vehicle_type": "golf_cart"
    ///         }
    ///     }
    /// }).as_object().unwrap().to_owned();
    ///
    /// // Without options
    /// let request_generator_no_opts = ValhallaHttpRequestGenerator::new(
    ///     "https://api.stadiamaps.com/route/v1?api_key=YOUR-API-KEY",
    ///     "low_speed_vehicle",
    ///     Map::new()
    /// );
    ///
    /// // With options
    /// let request_generator_opts = ValhallaHttpRequestGenerator::new(
    ///     "https://api.stadiamaps.com/route/v1?api_key=YOUR-API-KEY",
    ///     "low_speed_vehicle",
    ///     options
    /// );
    /// ```
    pub fn new<U: Into<String>, P: Into<String>>(
        endpoint_url: U,
        profile: P,
        options: Map<String, JsonValue>,
    ) -> Self {
        Self {
            endpoint_url: endpoint_url.into(),
            profile: profile.into(),
            options,
        }
    }

    /// Creates a new Valhalla request generator given an endpoint URL, a profile name,
    /// and options to include in the request JSON.
    /// Options in this constructor are a JSON fragment representing any
    /// options you want to add along with the request.
    ///
    /// # Examples
    ///
    /// ```
    /// # use ferrostar::routing_adapters::valhalla::ValhallaHttpRequestGenerator;
    /// let options = r#"{
    ///     "costing_options": {
    ///         "low_speed_vehicle": {
    ///             "vehicle_type": "golf_cart"
    ///         }
    ///     }
    /// }"#;
    ///
    /// // Without options
    /// let request_generator_no_opts = ValhallaHttpRequestGenerator::with_options_json(
    ///     "https://api.stadiamaps.com/route/v1?api_key=YOUR-API-KEY",
    ///     "low_speed_vehicle",
    ///     None,
    /// );
    ///
    /// // With options
    /// let request_generator_opts = ValhallaHttpRequestGenerator::with_options_json(
    ///     "https://api.stadiamaps.com/route/v1?api_key=YOUR-API-KEY",
    ///     "low_speed_vehicle",
    ///     Some(options),
    /// );
    /// ```
    pub fn with_options_json<U: Into<String>, P: Into<String>>(
        endpoint_url: U,
        profile: P,
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
            options: parsed_options,
        })
    }
}

impl RouteRequestGenerator for ValhallaHttpRequestGenerator {
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
            // TODO: Figure out how / if we want waypoint properties for the initial location
            let mut start = json!({
                "lat": user_location.coordinates.lat,
                "lon": user_location.coordinates.lng,
                // TODO: Street side tolerance as a tunable
                "street_side_tolerance": core::cmp::max(5, user_location.horizontal_accuracy as u16),
            });
            // TODO: Tunable to decide whether we care about course, and how accurate it needs to be
            if let Some(course) = user_location.course_over_ground {
                start["heading"] = course.degrees.into();
            }

            let waypoints: Vec<_> = waypoints
                .into_iter()
                .map(|waypoint| {
                    let intermediate = json!({
                        "lat": waypoint.coordinate.lat,
                        "lon": waypoint.coordinate.lng,
                        "type": match waypoint.kind {
                            WaypointKind::Break => "break",
                            WaypointKind::Via => "via",
                        },
                    });
                    Ok(merge_optional_waypoint_properties(
                        intermediate,
                        if let Some(props) = waypoint.properties.as_deref() {
                            serde_json::from_slice(props)?
                        } else {
                            None
                        },
                    ))
                })
                .collect::<Result<_, RoutingRequestGenerationError>>()?;

            let locations: Vec<JsonValue> = core::iter::once(start).chain(waypoints).collect();

            // NOTE: We currently use the OSRM format, as it is the richest one.
            // Though it would be nice to use PBF if we can get the required data.
            // However, certain info (like banners) are only available in the OSRM format.
            // TODO: Trace attributes as we go rather than pulling a fat payload upfront that we might ditch later?
            let mut args = json!({
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
                "banner_instructions": true,
                "voice_instructions": true,
                "costing": &self.profile,
                "locations": locations,
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

fn merge_optional_waypoint_properties(
    location: JsonValue,
    waypoint_properties: Option<ValhallaWaypointProperties>,
) -> JsonValue {
    let Some(ValhallaWaypointProperties {
        heading,
        heading_tolerance,
        minimum_reachability,
        radius,
        preferred_side,
        display_coordinate,
        search_cutoff,
        node_snap_tolerance,
        street_side_tolerance,
        street_side_max_distance,
        street_side_cutoff,
        search_filter,
    }) = waypoint_properties
    else {
        return location;
    };

    let mut result = location;

    if let Some(heading) = heading {
        result["heading"] = heading.into();
    }

    if let Some(heading_tolerance) = heading_tolerance {
        result["heading_tolerance"] = heading_tolerance.into();
    }

    if let Some(minimum_reachability) = minimum_reachability {
        result["minimum_reachability"] = minimum_reachability.into();
    }

    if let Some(radius) = radius {
        result["radius"] = radius.into();
    }

    if let Some(preferred_side) = preferred_side {
        result["preferred_side"] =
            serde_json::to_value(&preferred_side).expect("This should never fail");
    }

    if let Some(display_coordinate) = display_coordinate {
        result["display_lat"] = display_coordinate.lat.into();
        result["display_lon"] = display_coordinate.lng.into();
    }

    if let Some(search_cutoff) = search_cutoff {
        result["search_cutoff"] = search_cutoff.into();
    }

    if let Some(node_snap_tolerance) = node_snap_tolerance {
        result["node_snap_tolerance"] = node_snap_tolerance.into();
    }

    if let Some(street_side_tolerance) = street_side_tolerance {
        result["street_side_tolerance"] = street_side_tolerance.into();
    }

    if let Some(street_side_max_distance) = street_side_max_distance {
        result["street_side_max_distance"] = street_side_max_distance.into();
    }

    if let Some(street_side_cutoff) = street_side_cutoff {
        result["street_side_cutoff"] =
            serde_json::to_value(&street_side_cutoff).expect("This should never fail");
    }

    if let Some(search_filter) = search_filter {
        result["search_filter"] =
            serde_json::to_value(&search_filter).expect("This should never fail");
    }

    result
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::{CourseOverGround, GeographicCoordinate};
    use assert_json_diff::assert_json_include;
    use serde_json::{from_slice, json};
    use std::sync::LazyLock;

    #[cfg(all(feature = "std", not(feature = "web-time")))]
    use std::time::SystemTime;
    #[cfg(feature = "web-time")]
    use web_time::SystemTime;

    const ENDPOINT_URL: &str = "https://api.stadiamaps.com/route/v1";
    const COSTING: &str = "bicycle";
    const USER_LOCATION: UserLocation = UserLocation {
        coordinates: GeographicCoordinate { lat: 0.0, lng: 0.0 },
        horizontal_accuracy: 6.0,
        course_over_ground: None,
        timestamp: SystemTime::UNIX_EPOCH,
        speed: None,
    };
    const USER_LOCATION_WITH_COURSE: UserLocation = UserLocation {
        coordinates: GeographicCoordinate { lat: 0.0, lng: 0.0 },
        horizontal_accuracy: 6.0,
        course_over_ground: Some(CourseOverGround {
            degrees: 42,
            accuracy: Some(12),
        }),
        timestamp: SystemTime::UNIX_EPOCH,
        speed: None,
    };
    static WAYPOINTS: LazyLock<[Waypoint; 2]> = LazyLock::new(|| {
        [
            Waypoint {
                coordinate: GeographicCoordinate { lat: 0.0, lng: 1.0 },
                kind: WaypointKind::Break,
                properties: None,
            },
            Waypoint::new_with_valhalla_properties(
                GeographicCoordinate { lat: 2.0, lng: 3.0 },
                WaypointKind::Break,
                ValhallaWaypointProperties {
                    preferred_side: Some(ValhallaWaypointPreferredSide::Same),
                    search_filter: Some(ValhallaLocationSearchFilter {
                        exclude_bridge: Some(true),
                        min_road_class: Some(ValhallaRoadClass::Residential),
                        ..Default::default()
                    }),
                    ..Default::default()
                },
            ),
        ]
    });

    #[test]
    fn not_enough_locations() {
        let generator = ValhallaHttpRequestGenerator::new(ENDPOINT_URL, COSTING, Map::new());

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
        let generator =
            ValhallaHttpRequestGenerator::with_options_json(ENDPOINT_URL, COSTING, options_json)
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
                "The Valhalla HTTP request generator currently only generates POST requests"
            ),
            Err(e) => {
                println!("Failed to generate request: {:?}", e);
                json!(null)
            }
        }
    }

    #[test]
    fn request_body_without_course() {
        let body_json = generate_body(USER_LOCATION, WAYPOINTS.to_vec(), None);

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
                ],
            })
        );
    }

    #[test]
    fn request_body_with_course() {
        let body_json = generate_body(USER_LOCATION_WITH_COURSE, WAYPOINTS.to_vec(), None);

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
                    },
                    {
                        "lat": 0.0,
                        "lon": 1.0
                    },
                    {
                        "lat": 2.0,
                        "lon": 3.0,
                        "preferred_side": "same",
                        "search_filter": {
                            "exclude_bridge": true,
                            "min_road_class": "residential",
                        }
                    }
                ],
            })
        );
    }

    #[test]
    fn request_body_without_costing_options() {
        let body_json = generate_body(USER_LOCATION, WAYPOINTS.to_vec(), None);

        assert!(body_json["costing_options"].is_null());
    }

    #[test]
    #[should_panic]
    fn request_body_invalid_costing_options() {
        // Valid JSON, but it's not an object.
        let body_json = generate_body(
            USER_LOCATION,
            WAYPOINTS.to_vec(),
            Some(r#"["costing_options"]"#),
        );

        assert!(body_json["costing_options"].is_null());
    }

    #[test]
    fn request_body_with_costing_options() {
        let body_json = generate_body(
            USER_LOCATION,
            WAYPOINTS.to_vec(),
            Some(r#"{"costing_options": {"bicycle": {"bicycle_type": "Road"}}}"#),
        );

        assert_json_include!(
            actual: body_json,
            expected: json!({
                "costing_options": {
                    "bicycle": {
                        "bicycle_type": "Road",
                    },
                },
            })
        );
    }

    #[test]
    fn request_body_with_multiple_options() {
        let body_json = generate_body(
            USER_LOCATION,
            WAYPOINTS.to_vec(),
            Some(r#"{"units": "mi", "costing_options": {"bicycle": {"bicycle_type": "Road"}}}"#),
        );

        assert_json_include!(
            actual: body_json,
            expected: json!({
                "costing_options": {
                    "bicycle": {
                        "bicycle_type": "Road",
                    },
                },
                "units": "mi"
            })
        );
    }

    #[test]
    fn request_body_with_invalid_horizontal_accuracy() {
        let generator = ValhallaHttpRequestGenerator::new(ENDPOINT_URL, COSTING, Map::new());
        let location = UserLocation {
            coordinates: GeographicCoordinate { lat: 0.0, lng: 0.0 },
            horizontal_accuracy: -6.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None,
        };

        let RouteRequest::HttpPost {
            url: request_url,
            headers,
            body,
        } = generator
            .generate_request(location, WAYPOINTS.to_vec())
            .unwrap()
        else {
            unreachable!(
                "The Valhalla HTTP request generator currently only generates POST requests"
            );
        };

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
                        "preferred_side": "same",
                        "search_filter": {
                            "exclude_bridge": true,
                            "min_road_class": "residential",
                        }
                    }
                ],
            })
        );
    }
}
