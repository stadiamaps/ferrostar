import CoreLocation
import FerrostarCarPlayUI
import FerrostarCore
import FerrostarCoreFFI
import SwiftUI

enum AppDefaults {
    static let initialLocation = CLLocation(latitude: 37.332726, longitude: -122.031790)
    static let mapStyleURL =
        URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(APIKeys.shared.stadiaMapsAPIKey)")!
}

enum DemoAppError: Error {
    case noUserLocation
    case noRoutes
    case other(Error)
}

extension FerrostarCore {
    convenience init(locationProvider: LocationProviding) throws {
        // Configure the navigation session.
        // You have a lot of flexibility here based on your use case.
        let config = SwiftNavigationControllerConfig(
            waypointAdvance: .waypointWithinRange(100.0),
            stepAdvanceCondition: stepAdvanceDistanceEntryAndExit(
                minimumHorizontalAccuracy: 32,
                distanceToEndOfStep: 10,
                distanceAfterEndStep: 5 // Note this condition should be very close to the step end as it'll hold the
                // puck at the step until met.
            ),
            arrivalStepAdvanceCondition: stepAdvanceDistanceToEndOfStep(distance: 10, minimumHorizontalAccuracy: 32),
            routeDeviationTracking: .staticThreshold(minimumHorizontalAccuracy: 25, maxAcceptableDeviation: 20),
            snappedLocationCourseFiltering: .snapToRoute
        )

        try self.init(
            valhallaEndpointUrl: URL(
                string: "https://api.stadiamaps.com/route/v1?api_key=\(APIKeys.shared.stadiaMapsAPIKey)"
            )!,
            profile: "bicycle",
            locationProvider: locationProvider,
            navigationControllerConfig: config,
            options: ["costing_options": ["bicycle": ["use_roads": 0.2]]],
            // This is how you can set up annotation publishing;
            // We provide "extended OSRM" support out of the box,
            // but this is fully extendable!
            annotation: AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM()
        )
    }
}

extension CLLocation {
    var simulatedLocationProvider: SimulatedLocationProvider {
        let simulated = SimulatedLocationProvider(location: self)
        simulated.warpFactor = 2
        return simulated
    }
}

/// This is a shared core where ferrostar lives
class AppEnvironment: ObservableObject {
    let locationProvider: SwitchableLocationProvider
    @Published var ferrostarCore: FerrostarCore
    @Published var camera = SharedMapViewCamera(camera: .center(AppDefaults.initialLocation.coordinate, zoom: 14))

    let navigationDelegate = NavigationDelegate()

    init(initialLocation: CLLocation = AppDefaults.initialLocation) throws {
        locationProvider = SwitchableLocationProvider(
            simulated: initialLocation.simulatedLocationProvider,
            type: .simulated
        )

        ferrostarCore = try FerrostarCore(locationProvider: locationProvider)

        // NOTE: Not all applications will need a delegate. Read the NavigationDelegate documentation for details.
        ferrostarCore.delegate = navigationDelegate
    }

    /// This is an example function which gets a single route (or throws).
    /// NOTE: While the demo app only shows single route auto-selection,
    /// some vendors support alternate/multiple routes in a single request
    func getRoute() async throws -> Route {
        guard let userLocation = locationProvider.lastLocation else {
            throw DemoAppError.noUserLocation
        }

        let waypoints = locations.map { Waypoint(
            coordinate: GeographicCoordinate(lat: $0.coordinate.latitude, lng: $0.coordinate.longitude),
            kind: .break
        ) }

        let routes = try await ferrostarCore.getRoutes(initialLocation: userLocation,
                                                       waypoints: waypoints)

        guard let route = routes.first else {
            throw DemoAppError.noRoutes
        }

        try locationProvider.use(route: route)

        return route
    }

    func startNavigation(route: Route) throws {
        // Starts the navigation state machine.
        // It's worth having a look through the parameters,
        // as most of the configuration happens here.
        try ferrostarCore.startNavigation(route: route)
    }

    func stopNavigation() {
        ferrostarCore.stopNavigation()
    }

    func toggleLocationSimulation() {
        locationProvider.toggle()
    }
}
