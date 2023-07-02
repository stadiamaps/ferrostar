import FFI
import Foundation
import CoreLocation

enum FerrostarCoreError: Error, Equatable {
    /// The user has disabled location services for this app.
    case LocationServicesDisabled
    case UserLocationUnknown
    /// The route request from the route adapter has an invalid URL.
    ///
    /// This should never be encountered by end users of the library, and indicates a programming error
    /// in the route adapter.
    case InvalidRequestUrl
    /// The route adapter responded to our request without error, but with no routes.
    case NoRoutesFromAdapter
    /// Invalid (non-2xx) HTTP status
    case HTTPStatusCode(Int)
}

/// Receives events from ``FerrostarCore``.
///
/// This is the central point responsible for relaying updates back to the UI layer.
public protocol FerrostarCoreDelegate: AnyObject {
    /// Called whenever the user's location is updated.
    ///
    /// This location *may* be snapped to the route or road network.
    func core(_ core: FerrostarCore, didUpdateLocation snappedLocation: CLLocation, andHeading heading: CLHeading?)

    /// Called when the location manager failed to get the user's location.
    ///
    /// This is a serious error, and the UI layer should inform the user. They may need to check their settings
    /// to enable location services.
    func core(_ core: FerrostarCore, locationManagerFailedWithError error: Error)

    /// Called when the router found one or more candidate routes.
    ///
    /// This should most often result in either a UI for the user to select a route or a programmatic selection
    /// of a route, followed by a call to `startNavigation(route:)`.
    func core(_ core: FerrostarCore, foundRoutes routes: [FFI.Route])

    /// Called when no candidate routes could be retrieved from the router.
    ///
    /// Note that the error could be an underlying failure, such as an HTTP error, or even a case
    /// where there was no obvious error, but the route adapter returns no routes.
    func core(_ core: FerrostarCore, routingFailedWithError error: Error)
}


/// The Ferrostar core.
///
/// This is the entrypoint for end users of Ferrostar, and is responsible
/// for "driving" the navigation with location updates and other events.
///
/// The usual flow is for callers to configure an instance of the core, set a ``delegate``,
/// and reuse the core for as long as it makes sense (necessarily somewhat app-specific).
/// Note that it is the responsibility of the caller to ensure that the location manager is authorized to get
/// live user location with high precision.
///
/// Users will first want to call ``getRoutes(waypoints:userLocation:)``
/// to fetch a list of possible routes asynchronously. Upon successfully computing a set of
/// possible routes, one is selected, either interactively by the user, or programmatically.
/// The particulars will vary by app; do what makes the most sense for your user experience.
///
/// Finally, with a route selected, call ``startNavigation(route:)`` to start a session.
@objc public class FerrostarCore: NSObject {
    /// The delegate which will receive Ferrostar core events.
    public weak var delegate: FerrostarCoreDelegate?

    private let networkSession: URLRequestLoading
    private let routeAdapter: FFI.RouteAdapterProtocol
    private let locationProvider: LocationProviding
    private var navigationController: FFI.NavigationControllerProtocol?

    public init(routeAdapter: FFI.RouteAdapterProtocol, locationManager: LocationProviding, networkSession: URLRequestLoading) {
        self.routeAdapter = routeAdapter
        self.locationProvider = locationManager
        self.networkSession = networkSession

        super.init()

        // Location provider setup
        self.locationProvider.delegate = self
        
    }

    public convenience init(valhallaEndpointUrl: URL, profile: String, locationManager: LocationProviding, networkSession: URLRequestLoading = URLSession.shared) {
        let routeAdapter = FFI.RouteAdapter.newValhallaHttp(endpointUrl: valhallaEndpointUrl.absoluteString, profile: profile)
        self.init(routeAdapter: routeAdapter, locationManager: locationManager, networkSession: networkSession)
    }

    /// Tries to get routes visiting one or more waypoints starting from the initial location.
    ///
    /// Success and failure are communicated via ``delegate`` methods.
    public func getRoutes(waypoints: [CLLocationCoordinate2D], initialLocation: CLLocation) {
        do {
            let routeRequest = try routeAdapter.generateRequest(waypoints: [FFI.GeographicCoordinates(lat: initialLocation.coordinate.latitude, lng: initialLocation.coordinate.longitude)] + waypoints.map({ $0.geographicCoordinates }))

            switch (routeRequest) {
            case .httpPost(url: let url, headers: let headers, body: let body):
                guard let url = URL(string: url) else {
                    delegate?.core(self, routingFailedWithError: FerrostarCoreError.InvalidRequestUrl)
                    return
                }
                var urlRequest = URLRequest(url: url)
                urlRequest.httpMethod = "POST"
                for (header, value) in headers {
                    urlRequest.setValue(value, forHTTPHeaderField: header)
                }
                urlRequest.httpBody = Data(body)

                networkSession.loadData(with: urlRequest) { [self] data, response, error in
                    if let e = error {
                        delegate?.core(self, routingFailedWithError: e)
                    } else if let res = response as? HTTPURLResponse, res.statusCode < 200 || res.statusCode >= 300 {
                        delegate?.core(self, routingFailedWithError: FerrostarCoreError.HTTPStatusCode(res.statusCode))
                    } else if let data = data {
                        let uint8Data = [UInt8](data)
                        do {
                            let routes = try routeAdapter.parseResponse(response: uint8Data)

                            guard (!routes.isEmpty) else {
                                delegate?.core(self, routingFailedWithError: FerrostarCoreError.NoRoutesFromAdapter)
                                return
                            }

                            delegate?.core(self, foundRoutes: routes)
                        } catch {
                            delegate?.core(self, routingFailedWithError: error)
                        }
                    }
                }
            }
        } catch {
            delegate?.core(self, routingFailedWithError: error)
        }
    }

    /// Starts navigation with the given route. Any previous navigation session is dropped.
    public func startNavigation(route: Route) throws {
        // This is technically possible, so we need to check and throw, but
        // it should be rather difficult to get a location fix, get a route,
        // and then somehow this property go nil again.
        guard let location = locationProvider.location else {
            throw FerrostarCoreError.UserLocationUnknown
        }

        locationProvider.startUpdatingLocation()
        locationProvider.startUpdatingHeading()

        navigationController = NavigationController(lastUserLocation: location.userLocation, route: route)
    }

    /// Stops navigation and stops requesting location updates (to save battery).
    public func stopNavigation() {
        navigationController = nil
        locationProvider.stopUpdatingLocation()
        locationProvider.stopUpdatingHeading()
    }
}

extension FerrostarCore: LocationManagingDelegate {
    public func locationManager(_ manager: LocationProviding, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }

        // TODO: Decide how/where we want to handle speed info.

        navigationController?.updateUserLocation(location: location.userLocation)

        delegate?.core(self, didUpdateLocation: location, andHeading: manager.heading)
    }

    public func locationManager(_ manager: LocationProviding, didUpdateHeading newHeading: CLHeading) {
        if let location = manager.location {
            delegate?.core(self, didUpdateLocation: location, andHeading: newHeading)
        }
    }

    public func locationManager(_ manager: LocationProviding, didFailWithError error: Error) {
        delegate?.core(self, locationManagerFailedWithError: error)
    }
}
