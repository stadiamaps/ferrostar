use crate::{
    GeographicCoordinate, Route, RouteAdapter, RouteRequest, RoutingRequestGenerationError,
    RoutingResponseParseError,
};
use std::sync::Arc;

/// Manages the navigation lifecycle, requesting the initial route and reacting based on inputs
/// from (usually) foreign code like user location updates.
pub struct NavigationController {
    route_adapter: Arc<RouteAdapter>,
    /// The last known location of the user. For all intents and purposes, the "user" is assumed
    /// to be at the location reported by their device (phone, car, etc.)
    user_location: GeographicCoordinate,
    /// The list of waypoints that the user wants to visit on this trip.
    waypoints: Vec<GeographicCoordinate>,
}

impl NavigationController {
    pub fn new(
        route_adapter: Arc<RouteAdapter>,
        user_location: GeographicCoordinate,
        waypoints: Vec<GeographicCoordinate>,
    ) -> Self {
        Self {
            route_adapter,
            user_location,
            waypoints,
        }
    }

    pub fn generate_route_request(&self) -> Result<RouteRequest, RoutingRequestGenerationError> {
        let mut waypoints = self.waypoints.clone();
        waypoints.insert(0, self.user_location);

        self.route_adapter.generate_request(self.waypoints.clone())
    }

    pub fn parse_route_response(
        &self,
        response: Vec<u8>,
    ) -> Result<Vec<Route>, RoutingResponseParseError> {
        self.route_adapter.parse_response(response)
    }

    // TODO: Process user location updates etc.
}
