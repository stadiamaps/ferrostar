import FerrostarCoreFFI
import Foundation

public extension Route {
    /// Create a new Route directly from an OSRM route.
    ///
    /// This behavior uses the same internal decoders as the OsrmResponseParser. This function will
    /// automatically map & combine via and break waypoints from the route and waypoint data objects.
    ///
    /// - Parameters:
    ///   - route: The encoded JSON data for the OSRM route.
    ///   - waypoints: The encoded JSON data for the OSRM waypoints.
    ///   - precision: The polyline precision.
    static func initFromOsrm(route: Data, waypoints: Data, polylinePrecision: UInt32) throws -> Route {
        try createRouteFromOsrm(routeData: route, waypointData: waypoints, polylinePrecision: polylinePrecision)
    }

    func getPolyline(precision: UInt32) throws -> String {
        try getRoutePolyline(route: self, precision: precision)
    }
}
