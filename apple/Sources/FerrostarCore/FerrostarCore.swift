import CoreLocation
import FerrostarCoreFFI
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
    case getNewRoutes(waypoints: [Waypoint])
}

/// Receives events from ``FerrostarCore``.
///
/// This is the central point responsible for relaying updates back to the application.
public protocol FerrostarCoreDelegate: AnyObject {
    /// Called when the core detects that the user has deviated from the route.
    ///
    /// This hook enables app developers to take the most appropriate corrective action.
    func core(
        _ core: FerrostarCore,
        correctiveActionForDeviation deviationInMeters: Double,
        remainingWaypoints waypoints: [Waypoint]
    ) -> CorrectiveAction

    /// Called when the core has loaded alternate routes.
    ///
    /// The developer may decide whether or not to act on this information given the current trip state.
    /// This is currently used for recalculation when the user diverges from the route, but can be extended for other
    /// uses in the future.
    /// Note that the `isCalculatingNewRoute` property of ``NavigationState`` will be true until this method returns.
    /// Delegates may thus rely on this state introspection to decide what action to take given alternate routes.
    func core(_ core: FerrostarCore, loadedAlternateRoutes routes: [Route])
}

/// This is the entrypoint for end users of Ferrostar on iOS, and is responsible
/// for "driving" the navigation with location updates and other events.
///
/// The usual flow is for callers to configure an instance of the core, set a ``delegate``,
/// and reuse the instance for as long as it makes sense (necessarily somewhat app-specific).
/// You can first call ``getRoutes(initialLocation:waypoints:)``
/// to fetch a list of possible routes asynchronously. After selecting a suitable route (either interactively by the
/// user, or programmatically), call ``startNavigation(route:config:)`` to start a session.
///
/// NOTE: it is the responsibility of the caller to ensure that the location provider is authorized to get
/// live user location with high precision.
// TODO: See about making FerrostarCore its own actor; then we can verify that we've published things back on the main actor. Need to see if this is possible with obj-c interop. See https://github.com/apple/swift-evolution/blob/main/proposals/0306-actors.md#actor-interoperability-with-objective-c
@objc public class FerrostarCore: NSObject, ObservableObject {
    /// The delegate which will receive Ferrostar core events.
    public weak var delegate: FerrostarCoreDelegate?

    /// The spoken instruction observer; responsible for text-to-speech announcements.
    public var spokenInstructionObserver: SpokenInstructionObserver?

    /// The minimum time to wait before initiating another route recalculation.
    ///
    /// This matters in the case that a user is off route, the framework calculates a new route,
    /// and the user is determined to still be off the new route.
    /// This adds a minimum delay (default 5 seconds).
    public var minimumTimeBeforeRecalculaton: TimeInterval = 5

    /// The observable state of the model (for easy binding in SwiftUI views).
    @Published public private(set) var state: NavigationState?

    private let networkSession: URLRequestLoading
    private let routeProvider: RouteProvider
    public let locationProvider: LocationProviding
    private var navigationController: NavigationControllerProtocol?
    private var routeRequestInFlight = false
    private var lastAutomaticRecalculation: Date? = nil
    private var lastLocation: UserLocation? = nil
    private var recalculationTask: Task<Void, Never>?
    private var queuedUtteranceIDs: Set<UUID> = Set()

    private var config: SwiftNavigationControllerConfig

    /// Initializes a core instance with the given parameters.
    ///
    /// This designated initializer is the most flexible, but the convenience ones may be easier to use.
    /// for common configuraitons.
    public init(
        routeProvider: RouteProvider,
        locationProvider: LocationProviding,
        navigationControllerConfig: SwiftNavigationControllerConfig,
        networkSession: URLRequestLoading
    ) {
        self.routeProvider = routeProvider
        self.locationProvider = locationProvider
        config = navigationControllerConfig
        self.networkSession = networkSession

        super.init()

        // Location provider setup
        locationProvider.delegate = self
    }

    /// Initializes a core instance for a Valhalla API accessed over HTTP.
    ///
    /// - Parameters
    ///   - valhallaEndpointUrl: The URL of the Valhalla endpoint you're trying to hit for route requests. If necessary,
    /// include your API key here.
    ///   - profile: The Valhalla costing model to use for route requests.
    ///   - navigationControllerConfig: Configuration of the navigation session.
    ///   - options: A dictionary of options to include in the request. The Valhalla request generator sets several
    /// automatically (like `format`), but this lets you add arbitrary options so you can access the full API.
    ///   - networkSession: The network session to use. Don't set this unless you need to replace the networking stack
    /// (ex: for testing).
    public convenience init(
        valhallaEndpointUrl: URL,
        profile: String,
        locationProvider: LocationProviding,
        navigationControllerConfig: SwiftNavigationControllerConfig,
        options: [String: Any] = [:],
        networkSession: URLRequestLoading = URLSession.shared
    ) throws {
        guard let jsonOptions = try String(
            data: JSONSerialization.data(withJSONObject: options),
            encoding: .utf8
        ) else {
            throw InstantiationError.OptionsJsonParseError
        }

        let adapter = try RouteAdapter.newValhallaHttp(
            endpointUrl: valhallaEndpointUrl.absoluteString,
            profile: profile,
            optionsJson: jsonOptions
        )
        self.init(
            routeProvider: .routeAdapter(adapter),
            locationProvider: locationProvider,
            navigationControllerConfig: navigationControllerConfig,
            networkSession: networkSession
        )
    }

    public convenience init(
        routeAdapter: RouteAdapterProtocol,
        locationProvider: LocationProviding,
        navigationControllerConfig: SwiftNavigationControllerConfig,
        networkSession: URLRequestLoading = URLSession.shared
    ) {
        self.init(
            routeProvider: .routeAdapter(routeAdapter),
            locationProvider: locationProvider,
            navigationControllerConfig: navigationControllerConfig,
            networkSession: networkSession
        )
    }

    public convenience init(
        customRouteProvider: CustomRouteProvider,
        locationProvider: LocationProviding,
        navigationControllerConfig: SwiftNavigationControllerConfig,
        networkSession: URLRequestLoading = URLSession.shared
    ) {
        self.init(
            routeProvider: .customProvider(customRouteProvider),
            locationProvider: locationProvider,
            navigationControllerConfig: navigationControllerConfig,
            networkSession: networkSession
        )
    }

    /// Tries to get routes visiting one or more waypoints starting from the initial location.
    ///
    /// Success and failure are communicated via ``delegate`` methods.
    public func getRoutes(initialLocation: UserLocation, waypoints: [Waypoint]) async throws -> [Route] {
        routeRequestInFlight = true

        defer {
            routeRequestInFlight = false
        }

        switch routeProvider {
        case let .customProvider(provider):
            return try await provider.getRoutes(userLocation: initialLocation, waypoints: waypoints)
        case let .routeAdapter(routeAdapter):
            let routeRequest = try routeAdapter.generateRequest(
                userLocation: initialLocation,
                waypoints: waypoints
            )

            let urlRequest = try routeRequest.urlRequest
            let (data, response) = try await networkSession.loadData(with: urlRequest)

            if let res = response as? HTTPURLResponse, res.statusCode < 200 || res.statusCode >= 300 {
                throw FerrostarCoreError.httpStatusCode(res.statusCode)
            } else {
                let routes = try routeAdapter.parseResponse(response: data)

                return routes
            }
        }
    }

    /// Starts navigation with the given route. Any previous navigation session is dropped.
    ///
    /// - Parameters:
    ///   - route: The route to navigate.
    ///   - config: Override the configuration for the navigation session. This was provided on init.
    public func startNavigation(route: Route, config: SwiftNavigationControllerConfig? = nil) throws {
        // This is technically possible, so we need to check and throw, but
        // it should be rather difficult to get a location fix, get a route,
        // and then somehow this property go nil again.
        guard let location = locationProvider.lastLocation else {
            throw FerrostarCoreError.userLocationUnknown
        }
        // TODO: We should be able to circumvent this and simply start updating, wait and start nav.

        // Apply the new config if one was provided to override.
        self.config = config ?? self.config

        // Configure the navigation controller. This is required to build the initial state.
        let controller = NavigationController(route: route, config: self.config.ffiValue)
        navigationController = controller

        locationProvider.startUpdating()

        state = NavigationState(
            tripState: controller.getInitialState(location: location),
            routeGeometry: route.geometry
        )

        DispatchQueue.main.async {
            self.update(newState: controller.getInitialState(location: location), location: location)
        }
    }

    public func advanceToNextStep() {
        guard let controller = navigationController, let tripState = state?.tripState, let lastLocation else {
            return
        }

        let newState = controller.advanceToNextStep(state: tripState)
        update(newState: newState, location: lastLocation)
    }

    // TODO: Ability to pause without totally stopping and clearing state

    /// Stops navigation and stops requesting location updates (to save battery).
    public func stopNavigation() {
        navigationController = nil
        state = nil
        queuedUtteranceIDs.removeAll()
        locationProvider.stopUpdating()
        spokenInstructionObserver?.stopAndClearQueue()
    }

    /// Internal state update.
    ///
    /// You should call this rather than setting properties directly
    private func update(newState: TripState, location: UserLocation) {
        DispatchQueue.main.async {
            self.state?.tripState = newState

            switch newState {
            case let .navigating(
                currentStepGeometryIndex: _,
                snappedUserLocation: _,
                remainingSteps: _,
                remainingWaypoints: remainingWaypoints,
                progress: _,
                deviation: deviation,
                visualInstruction: _,
                spokenInstruction: spokenInstruction,
                annotationJson: _
            ):
                switch deviation {
                case .noDeviation:
                    // No action
                    break
                case let .offRoute(deviationFromRouteLine: deviationFromRouteLine):
                    guard !self.routeRequestInFlight,
                          self.lastAutomaticRecalculation?.timeIntervalSinceNow ?? -TimeInterval
                          .greatestFiniteMagnitude < -self
                          .minimumTimeBeforeRecalculaton
                    else {
                        break
                    }

                    switch self.delegate?.core(
                        self,
                        correctiveActionForDeviation: deviationFromRouteLine,
                        remainingWaypoints: remainingWaypoints
                    ) ?? .getNewRoutes(waypoints: remainingWaypoints) {
                    case .doNothing:
                        break
                    case let .getNewRoutes(waypoints):
                        self.state?.isCalculatingNewRoute = true
                        self.recalculationTask = Task {
                            do {
                                let routes = try await self.getRoutes(
                                    initialLocation: location,
                                    waypoints: waypoints
                                )
                                if let delegate = self.delegate {
                                    delegate.core(self, loadedAlternateRoutes: routes)
                                } else if let route = routes.first {
                                    // Default behavior when no delegate is assigned:
                                    // accept the first route, as this is what most users want when they go off route.
                                    try self.startNavigation(route: route, config: self.config)
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
                }

                if let spokenInstruction, !self.queuedUtteranceIDs.contains(spokenInstruction.utteranceId) {
                    self.queuedUtteranceIDs.insert(spokenInstruction.utteranceId)

                    // This sholud not happen on the main queue as it can block;
                    // we'll probably remove the need for this eventually
                    // by making FerrostarCore its own actor
                    DispatchQueue.global(qos: .default).async {
                        self.spokenInstructionObserver?.spokenInstructionTriggered(spokenInstruction)
                    }
                }
            default:
                break
            }
        }
    }
}

extension FerrostarCore: LocationManagingDelegate {
    @MainActor
    public func locationManager(_: LocationProviding, didUpdateLocations locations: [UserLocation]) {
        guard let location = locations.last,
              let state = state?.tripState,
              let newState = navigationController?.updateUserLocation(location: location, state: state)
        else {
            return
        }

        lastLocation = location

        update(newState: newState, location: location)
    }

    public func locationManager(_: LocationProviding, didUpdateHeading _: Heading) {
        // TODO: Make use of heading in TripState?
//        state?.heading = newHeading
    }

    public func locationManager(_: LocationProviding, didFailWithError _: Error) {
        // TODO: Decide if/how to propagate this upstream later.
        // For initial releases, we simply assume that the developer has requested the correct permissions
        // and ensure this before attempting to start location updates.
    }
}
