import FFI
import Foundation
import CoreLocation

enum FerrostarCoreError: Error {
    case LocationServicesDisabled
    case UserLocationUnknown
    case InvalidRequestUrl
}

public protocol FerrostarCoreDelegate: AnyObject {
    /// Called whenever the user's location is updated.
    ///
    /// Note that this location may be snapped to the route or road network.
    func core(_ core: FerrostarCore, didUpdateLocation snappedLocation: CLLocation, andHeading heading: CLHeading?)
    func core(_ core: FerrostarCore, locationManagerFailedWithError error: Error)

    func core(_ core: FerrostarCore, foundRoutes routes: [FFI.Route])
    func core(_ core: FerrostarCore, routingFailedWithError error: Error)
}


/// The Ferrostar core.
///
/// This is the entrypoint for end users of Ferrostar, and is responsible
/// for "driving" the navigation with location updates and other events.
///
/// The usual flow is for callers to configure an instance of the core, set a ``delegate``,
/// and reuse the core for as long as it makes sense (necessarily somewhat app-specific).
///
/// Users will first want to call ``getRoutes(waypoints:onCompletion:onError:)``
/// to fetch a list of possible routes asynchronously. Upon successfully computing a set of
/// possible routes, one is selected, either interactively by the user, or programmatically.
/// The particulars will vary by app; do what makes the most sense for your user experience.
///
/// Finally, with a route selected, call ``startNavigation(route:)`` to start a session.
@objc public class FerrostarCore: NSObject {
    /// The delegate which will receive Ferrostar core events.
    public weak var delegate: FerrostarCoreDelegate?

    private let routeAdapter: FFI.RouteAdapterProtocol
    private let locationManager: CLLocationManager
    private var navigationController: FFI.NavigationControllerProtocol?

    public init(routeAdapter: FFI.RouteAdapter) {
        self.routeAdapter = routeAdapter
        self.locationManager = CLLocationManager()

        super.init()

        // Location manager setup
        self.locationManager.delegate = self
        self.locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation

        switch (self.locationManager.authorizationStatus) {
        case .notDetermined:
            self.locationManager.requestWhenInUseAuthorization()
        default:
            break  // No action
        }
    }

    public convenience init(valhallaEndopointUrl: URL, profile: String) {
        let routeAdapter = FFI.RouteAdapter.newValhallaHttp(endpointUrl: valhallaEndopointUrl.absoluteString, profile: profile)
        self.init(routeAdapter: routeAdapter)
    }

    /// Tries to get routes visiting one or more waypoints starting from the user's location.
    ///
    /// Success and failure are communicated via delegate methods.
    public func getRoutes(waypoints: [CLLocationCoordinate2D], userLocation: CLLocation) {
        do {
            let routeRequest = try routeAdapter.generateRequest(waypoints: [FFI.GeographicCoordinates(lat: userLocation.coordinate.latitude, lng: userLocation.coordinate.longitude)] + waypoints.map({ $0.geographicCoordinates }))

            // TODO: Make this more mockable
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

                let task = URLSession.shared.dataTask(with: urlRequest) { [self] data, response, error in
                    if let e = error {
                        delegate?.core(self, routingFailedWithError: e)
                    } else if let data = data {
                        let uint8Data = [UInt8](data)
                        do {
                            let routes = try routeAdapter.parseResponse(response: uint8Data)

                            delegate?.core(self, foundRoutes: routes)
                        } catch {
                            delegate?.core(self, routingFailedWithError: error)
                        }
                    }
                }
                task.resume()
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
        guard let location = locationManager.location else {
            throw FerrostarCoreError.UserLocationUnknown
        }

        let authorizedStatuses = [CLAuthorizationStatus.authorizedWhenInUse, CLAuthorizationStatus.authorizedAlways]

        guard authorizedStatuses.contains(locationManager.authorizationStatus) else {
            throw FerrostarCoreError.LocationServicesDisabled
        }

        locationManager.startUpdatingLocation()
        locationManager.startUpdatingHeading()

        navigationController = NavigationController(lastUserLocation: location.userLocation, route: route)
    }

    /// Stops navigation and stops requesting location updates (to save battery).
    public func stopNavigation() {
        navigationController = nil
        locationManager.stopUpdatingLocation()
        locationManager.stopUpdatingHeading()
    }
}

extension FerrostarCore: CLLocationManagerDelegate {
    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        delegate?.core(self, locationManagerFailedWithError: error)
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }

        // TODO: Decide how/where we want to handle speed info.

        navigationController?.updateUserLocation(location: location.userLocation)
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        if let location = manager.location {
            delegate?.core(self, didUpdateLocation: location, andHeading: newHeading)
        }
    }
}
