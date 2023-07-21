import CoreLocation
import UniFFI
import Foundation

enum FerrostarCoreError: Error, Equatable {
    /// The user has disabled location services for this app.
    case locationServicesDisabled
    case userLocationUnknown
    /// The route request from the route adapter has an invalid URL.
    ///
    /// This should never be encountered by end users of the library, and indicates a programming error
    /// in the route adapter.
    case invalidRequestUrl
    /// Invalid (non-2xx) HTTP status
    case httpStatusCode(Int)
}

/// A route.
///
/// This type exports a nice Swift-y version of the route with some convenience properties.
///
/// This wrapper approach is unfortunaetly necessary beacuse Swift packages cannot yet
/// re-export inner modules. The types used in signatures have the information available, and values
/// returned from functions can be inspected, but the type name cannot be explicitly used in variable or
/// function signatures. So to work around the issue, we export a wrapper type that *can* be
/// referenced in other packages (like the UI) which need to hang on to the route without getting the whole
/// FFI.
public struct Route {
    let inner: UniFFI.Route

    var geometry: [CLLocationCoordinate2D] {
        inner.geometry.map { point in
            CLLocationCoordinate2D(latitude: point.lat, longitude: point.lng)
        }
    }
}

/// Receives events from ``FerrostarCore``.
///
/// This is the central point responsible for relaying updates back to the application.
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

    /// Called when the core gives us an updated navigation state.
    ///
    /// This method will be called whenever the user's location changes significantly enough to have an effect
    /// on the navigation state that is important enough to require a decision or UI update (quite often in the
    /// case of a moving vehicle).
    ///
    /// This is *probably* not the final interface for this function but it's something to start with.
    func core(_ core: FerrostarCore, didUpdateNavigationState update: NavigationStateUpdate)
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
    private let routeAdapter: UniFFI.RouteAdapterProtocol
    private let locationProvider: LocationProviding
    private var navigationController: UniFFI.NavigationControllerProtocol?

    public init(routeAdapter: UniFFI.RouteAdapterProtocol, locationManager: LocationProviding, networkSession: URLRequestLoading) {
        self.routeAdapter = routeAdapter
        locationProvider = locationManager
        self.networkSession = networkSession

        super.init()

        // Location provider setup
        locationProvider.delegate = self
    }

    public convenience init(valhallaEndpointUrl: URL, profile: String, locationManager: LocationProviding, networkSession: URLRequestLoading = URLSession.shared) {
        let routeAdapter = UniFFI.RouteAdapter.newValhallaHttp(endpointUrl: valhallaEndpointUrl.absoluteString, profile: profile)
        self.init(routeAdapter: routeAdapter, locationManager: locationManager, networkSession: networkSession)
    }

    /// Tries to get routes visiting one or more waypoints starting from the initial location.
    ///
    /// Success and failure are communicated via ``delegate`` methods.
    public func getRoutes(waypoints: [CLLocationCoordinate2D], initialLocation: CLLocation) async throws -> [Route] {
        let routeRequest = try routeAdapter.generateRequest(userLocation: initialLocation.userLocation, waypoints: waypoints.map { $0.geographicCoordinates })

        switch routeRequest {
        case let .httpPost(url: url, headers: headers, body: body):
            guard let url = URL(string: url) else {
                throw FerrostarCoreError.invalidRequestUrl
            }

            var urlRequest = URLRequest(url: url)
            urlRequest.httpMethod = "POST"
            for (header, value) in headers {
                urlRequest.setValue(value, forHTTPHeaderField: header)
            }
            urlRequest.httpBody = Data(body)

            let (data, response) = try await networkSession.loadData(with: urlRequest)

            if let res = response as? HTTPURLResponse, res.statusCode < 200 || res.statusCode >= 300 {
                throw FerrostarCoreError.httpStatusCode(res.statusCode)
            } else {
                let uint8Data = [UInt8](data)
                let routes = try routeAdapter.parseResponse(response: uint8Data)

                return routes.map { Route(inner: $0) }
            }
        }
    }

    /// Starts navigation with the given route. Any previous navigation session is dropped.
    public func startNavigation(route: Route) throws {
        // This is technically possible, so we need to check and throw, but
        // it should be rather difficult to get a location fix, get a route,
        // and then somehow this property go nil again.
        guard let location = locationProvider.location else {
            throw FerrostarCoreError.userLocationUnknown
        }

        locationProvider.startUpdatingLocation()
        locationProvider.startUpdatingHeading()

        navigationController = NavigationController(lastUserLocation: location.userLocation, route: route.inner)
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

        if let update = navigationController?.updateUserLocation(location: location.userLocation) {
            delegate?.core(self, didUpdateNavigationState: update)
        }

        delegate?.core(self, didUpdateLocation: location, andHeading: manager.heading)
    }

    public func locationManager(_ manager: LocationProviding, didUpdateHeading newHeading: CLHeading) {
        if let location = manager.location {
            delegate?.core(self, didUpdateLocation: location, andHeading: newHeading)
        }
    }

    public func locationManager(_: LocationProviding, didFailWithError error: Error) {
        delegate?.core(self, locationManagerFailedWithError: error)
    }
}
