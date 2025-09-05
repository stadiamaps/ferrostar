import CoreLocation
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI

class AppService {
    nonisolated(unsafe) static let shared = AppService()

    let locationProvider: SwitchableLocationProvider
    let ferrostarCore: FerrostarCore
    let navigationDelegate = NavigationDelegate()

    // In a normal app, this would be done differently.
    // This is just an easy way to share some global app state
    @Published private(set) var appState: DemoAppState = .idle

    init() {
        locationProvider = SwitchableLocationProvider(
            simulated: AppDefaults.initialLocation.simulatedLocationProvider,
            type: .simulated
        )

        ferrostarCore = try! FerrostarCore.initForDemo(locationProvider: locationProvider)
        ferrostarCore.delegate = navigationDelegate
    }

    // MARK: Service Functionality

    @discardableResult
    func fetchRoutes(
        origin: CLLocation,
        destination: CLLocationCoordinate2D
    ) async throws -> [Route] {
        let location = UserLocation(clLocation: origin)

        let coord = GeographicCoordinate(cl: destination)
        let waypoint = Waypoint(coordinate: coord, kind: .break)

        let routes = try await ferrostarCore.getRoutes(initialLocation: location, waypoints: [waypoint])
        appState = .routes(routes: routes)
        return routes
    }

    func selectRoute(_ route: Route) {
        appState = .selectedRoute(route)
    }

    func startNavigation() throws {
        guard case let .selectedRoute(route) = appState else { return }

        try ferrostarCore.startNavigation(route: route)
        appState = .navigating
    }

    func stopNavigation() {
        ferrostarCore.stopNavigation()
        appState = .idle
    }
}

// MARK: Conveniences

private extension CLLocation {
    var simulatedLocationProvider: SimulatedLocationProvider {
        let simulated = SimulatedLocationProvider(location: self)
        simulated.warpFactor = 2
        return simulated
    }
}

private extension FerrostarCore {
    static func initForDemo(locationProvider: LocationProviding) throws -> FerrostarCore {
        // Configure the navigation session.
        // You have a lot of flexibility here based on your use case.
        let config = SwiftNavigationControllerConfig(
            waypointAdvance: .waypointWithinRange(100.0),
            stepAdvanceCondition: stepAdvanceDistanceEntryAndExit(
                distanceToEndOfStep: 30,
                distanceAfterEndOfStep: 2,
                minimumHorizontalAccuracy: 32
            ),
            arrivalStepAdvanceCondition: stepAdvanceDistanceToEndOfStep(
                distance: 30,
                minimumHorizontalAccuracy: 32
            ),
            routeDeviationTracking: .staticThreshold(minimumHorizontalAccuracy: 25, maxAcceptableDeviation: 20),
            snappedLocationCourseFiltering: .snapToRoute
        )

        return try FerrostarCore(
            valhallaEndpointUrl: URL(
                string: "https://api.stadiamaps.com/route/v1?api_key=\(sharedAPIKeys.stadiaMapsAPIKey)"
            )!,
            profile: "bicycle",
            locationProvider: locationProvider,
            navigationControllerConfig: config,
            options: ["costing_options": ["bicycle": ["use_roads": 0.2]]],
            // This is how you can set up annotation publishing;
            // We provide "extended OSRM" support out of the box,
            // but this is fully extendable!
            annotation: AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM(),
            widgetProvider: FerrostarWidgetProvider()
        )
    }
}
