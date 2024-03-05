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

/// Corrective action to take when the user deviates from the route.
public enum CorrectiveAction {
    /// Don't do anything.
    ///
    /// Note that this is most commonly paired with no route deviation tracking as a formality.
    /// Think twice before using this as a mechanism for implementing your own logic outside of the provided framework,
    /// as doing so will mean you miss out on state updates around alternate route calculation.
    case doNothing
    /// Tells the core to fetch new routes from the route adapter.
    ///
    /// Once they are available, the delegate will be notified of the new routes.
    case getNewRoutes(waypoints: [CLLocationCoordinate2D])
}

/// Receives events from ``FerrostarCore``.
///
/// This is the central point responsible for relaying updates back to the application.
public protocol FerrostarCoreDelegate: AnyObject {
    /// Called when the core detects that the user has deviated from the route.
    ///
    /// This hook enables app developers to take the most appropriate corrective action.
    func core(_ core: FerrostarCore, correctiveActionForDeviation deviationInMeters: Double, remainingWaypoints waypoints: [CLLocationCoordinate2D]) -> CorrectiveAction

    /// Called when the core has loaded alternate routes.
    ///
    /// The developer may decide whether or not to act on this information given the current trip state.
    /// This is currently used for recalculation when the user diverges from the route, but can be extended for other uses in the future.
    /// Note that the `isCalculatingNewRoute` property of ``NavigationState`` will be true until this method returns.
    /// Delegates may thus rely on this state introspection to decide what action to take given alterante routes.
    func core(_ core: FerrostarCore, loadedAlternateRoutes routes: [Route])
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
/// Finally, with a route selected, call ``startNavigation(route:config:)`` to start a session.
@objc public class FerrostarCore: NSObject, ObservableObject {
    /// The delegate which will receive Ferrostar core events.
    public weak var delegate: FerrostarCoreDelegate?

    /// The minimum time to wait before initiating another route recalculation.
    ///
    /// This matters in the case that a user is off route, the framework calculates a new route,
    /// and the user is determined to still be off the new route.
    /// This adds a minimum delay (default 5 seconds).
    public var minimumTimeBeforeRecalculaton: TimeInterval = 5

    /// The observable state of the model (for easy binding in SwiftUI views).
    @Published public private(set) var state: NavigationState?

    private let networkSession: URLRequestLoading
    private let routeAdapter: UniFFI.RouteAdapterProtocol
    private let locationProvider: LocationProviding
    private var navigationController: UniFFI.NavigationControllerProtocol?
    private var tripState: UniFFI.TripState?
    private var routeRequestInFlight = false
    private var lastAutomaticRecalculation: Date? = nil
    private var recalculationTask: Task<(), Never>?
    private var isStarted: Bool = false
    
    private var config: NavigationControllerConfig?

    public init(routeAdapter: UniFFI.RouteAdapterProtocol, locationProvider: LocationProviding, networkSession: URLRequestLoading) {
        self.routeAdapter = routeAdapter
        self.locationProvider = locationProvider
        self.networkSession = networkSession

        super.init()

        // Location provider setup
        locationProvider.delegate = self
    }

    public convenience init(valhallaEndpointUrl: URL, profile: String, locationProvider: LocationProviding, networkSession: URLRequestLoading = URLSession.shared) {
        let routeAdapter = UniFFI.RouteAdapter.newValhallaHttp(endpointUrl: valhallaEndpointUrl.absoluteString, profile: profile)
        self.init(routeAdapter: routeAdapter, locationProvider: locationProvider, networkSession: networkSession)
    }

    /// Tries to get routes visiting one or more waypoints starting from the initial location.
    ///
    /// Success and failure are communicated via ``delegate`` methods.
    public func getRoutes(initialLocation: CLLocation, waypoints: [CLLocationCoordinate2D]) async throws -> [Route] {
        routeRequestInFlight = true

        defer {
            routeRequestInFlight = false
        }

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
            urlRequest.timeoutInterval = 15

            let (data, response) = try await networkSession.loadData(with: urlRequest)

            if let res = response as? HTTPURLResponse, res.statusCode < 200 || res.statusCode >= 300 {
                throw FerrostarCoreError.httpStatusCode(res.statusCode)
            } else {
                let routes = try routeAdapter.parseResponse(response: data)

                return routes.map { Route(inner: $0) }
            }
        }
    }

    /// Starts navigation with the given route. Any previous navigation session is dropped.
    public func startNavigation(route: Route, config: NavigationControllerConfig) throws {
        // This is technically possible, so we need to check and throw, but
        // it should be rather difficult to get a location fix, get a route,
        // and then somehow this property go nil again.
        guard let location = locationProvider.lastLocation else {
            throw FerrostarCoreError.userLocationUnknown
        }

        self.config = config

        locationProvider.startUpdating()

        state = NavigationState(snappedLocation: location, heading: locationProvider.lastHeading, fullRoute: route.geometry, steps: route.inner.steps)
        let controller = NavigationController(route: route.inner, config: config.ffiValue)
        navigationController = controller
        DispatchQueue.main.async {
            self.update(newState: controller.getInitialState(location: location.userLocation), location: location)
        }
    }

    /// Stops navigation and stops requesting location updates (to save battery).
    public func stopNavigation() {
        navigationController = nil
        state = nil
        tripState = nil
        locationProvider.stopUpdating()
    }

    /// Internal state update.
    ///
    /// You should call this rather than setting properties directly
    private func update(newState: UniFFI.TripState, location: CLLocation) {
        DispatchQueue.main.async {
            self.tripState = newState

            switch (newState) {
            case .navigating(snappedUserLocation: let snappedLocation, remainingSteps: let remainingSteps, remainingWaypoints: let remainingWaypoints, distanceToNextManeuver: let distanceToNextManeuver, deviation: let deviation):
                self.state?.snappedLocation = snappedLocation
                self.state?.courseOverGround = snappedLocation.courseOverGround
                self.state?.currentStep = remainingSteps.first
                // TODO: This isn't great; the core should probably just tell us which instruction to display
                self.state?.visualInstructions = remainingSteps.first?.visualInstructions.last(where: { instruction in
                    distanceToNextManeuver <= instruction.triggerDistanceBeforeManeuver
                })
                self.state?.distanceToNextManeuver = distanceToNextManeuver
                let clRemainingWaypoints = remainingWaypoints.map({ coord in
                    CLLocationCoordinate2D(geographicCoordinates: coord)
                })

    //                observableState?.spokenInstruction = currentStep.spokenInstruction.last(where: { instruction in
    //                    currentStepRemainingDistance <= instruction.triggerDistanceBeforeManeuver
    //                })

                switch (deviation) {
                case .noDeviation:
                    // No action
                    break
                case .offRoute(deviationFromRouteLine: let deviationFromRouteLine):
                    guard !self.routeRequestInFlight && self.lastAutomaticRecalculation?.timeIntervalSinceNow ?? 0 > -self.minimumTimeBeforeRecalculaton else {
                        break
                    }

                    switch (self.delegate?.core(self, correctiveActionForDeviation: deviationFromRouteLine, remainingWaypoints: clRemainingWaypoints) ?? .getNewRoutes(waypoints: clRemainingWaypoints)) {
                    case .doNothing:
                        break
                    case .getNewRoutes(let waypoints):
                        self.state?.isCalculatingNewRoute = true
                        self.recalculationTask = Task {
                            do {
                                let routes = try await self.getRoutes(initialLocation: location, waypoints: waypoints)
                                if let delegate = self.delegate {
                                    delegate.core(self, loadedAlternateRoutes: routes)
                                } else if let route = routes.first, let config = self.config {
                                    // Default behavior when no delegate is assigned:
                                    // accept the first route, as this is what most users want when they go off route.
                                    try self.startNavigation(route: route, config: config)
                                }
                            } catch {
                                // Do nothing; this exists to enable us to run what amounts to an "async defer"
                            }

                            await MainActor.run {
                                self.lastAutomaticRecalculation = Date()
                                self.state?.isCalculatingNewRoute = false
                            }
                        }
                    }

                    break
                }
            case .complete:
                // TODO: "You have arrived"?
                self.state?.visualInstructions = nil
                self.state?.snappedLocation = UserLocation(clLocation: location)
                self.state?.courseOverGround = CourseOverGround(course: location.course, courseAccuracy: location.courseAccuracy)
                self.state?.spokenInstruction = nil
            }
        }
    }
}

extension FerrostarCore: LocationManagingDelegate {
    @MainActor
    public func locationManager(_ manager: LocationProviding, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last,
              let state = tripState,
              let newState = navigationController?.updateUserLocation(location: location.userLocation, state: state) else {
            return
        }
        
        update(newState: newState, location: location)
    }

    public func locationManager(_ manager: LocationProviding, didUpdateHeading newHeading: CLHeading) {
        state?.heading = Heading(clHeading: newHeading)
    }

    public func locationManager(_: LocationProviding, didFailWithError error: Error) {
        // TODO: Decide if/how to propagate this upstream later.
        // For initial releases, we simply assume that the developer has requested the correct permissions
        // and ensure this before attempting to start location updates.
    }
}
