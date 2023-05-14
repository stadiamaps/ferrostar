import FFI
import Foundation
import CoreLocation


public class FerrostarCore {
    private let controller: FFI.NavigationController

    // Low-level interface
    public init(routeAdapter: FFI.RouteAdapter, initialUserLocation: CLLocation, waypoints: Array<CLLocationCoordinate2D>) {
        controller = FFI.NavigationController(routeAdapter: routeAdapter, userLocation: GeographicCoordinate(lat: initialUserLocation.coordinate.latitude, lng: initialUserLocation.coordinate.longitude), waypoints: waypoints.map { GeographicCoordinate(lat: $0.latitude, lng: $0.longitude) })
    }

    // High-level interface
    public convenience init(valhallaEndopointUrl: URL, profile: String, initialUserLocation: CLLocation, waypoints: Array<CLLocationCoordinate2D>) {
        let routeAdapter = FFI.RouteAdapter.newValhallaHttp(endpointUrl: valhallaEndopointUrl.absoluteString, profile: profile)
        self.init(routeAdapter: routeAdapter, initialUserLocation: initialUserLocation, waypoints: waypoints)
    }
}
