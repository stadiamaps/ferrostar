import CoreLocation
import FerrostarCoreFFI

public enum RouteProvider {
    case routeAdapter(RouteAdapterProtocol)
    case customProvider(CustomRouteProvider)
}

public protocol CustomRouteProvider {
    func getRoutes(userLocation: UserLocation, waypoints: [Waypoint]) async throws -> [Route]
}
