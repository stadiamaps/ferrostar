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
            stepAdvance: .relativeLineStringDistance(
                minimumHorizontalAccuracy: 32,
                specialAdvanceConditions: .minimumDistanceFromCurrentStepLine(10)
            ),
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
    var simulatedLocationProvider: LocationProviding {
        let simulated = SimulatedLocationProvider(location: self)
        simulated.warpFactor = 2
        return simulated
    }
}

/// This is a shared core where ferrostar lives
class AppEnvironment: ObservableObject {
    var locationProvider: LocationProviding
    @Published var ferrostarCore: FerrostarCore
    @Published var camera = SharedMapViewCamera(camera: .center(AppDefaults.initialLocation.coordinate, zoom: 14))

    let navigationDelegate = NavigationDelegate()

    init(initialLocation: CLLocation = AppDefaults.initialLocation) throws {
        locationProvider = initialLocation.simulatedLocationProvider

        ferrostarCore = try FerrostarCore(locationProvider: locationProvider)

        // NOTE: Not all applications will need a delegate. Read the NavigationDelegate documentation for details.
        ferrostarCore.delegate = navigationDelegate
    }

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

        if let simulated = locationProvider as? SimulatedLocationProvider {
            // This configures the simulator to the desired route.
            // The ferrostarCore.startNavigation will still start the location
            // provider/simulator.
            simulated.lastLocation = UserLocation(clCoordinateLocation2D: route.geometry.first!.clLocationCoordinate2D)
        }

        return route
    }

    func startNavigation(route: Route) throws {
        if let simulated = locationProvider as? SimulatedLocationProvider {
            // This configures the simulator to the desired route.
            // The ferrostarCore.startNavigation will still start the location
            // provider/simulator.
            try simulated.setSimulatedRoute(route, resampleDistance: 5)
            print("DemoApp: setting route to be simulated")
        }

        // Starts the navigation state machine.
        // It's worth having a look through the parameters,
        // as most of the configuration happens here.
        try ferrostarCore.startNavigation(route: route)
    }

    func stopNavigation() {
        ferrostarCore.stopNavigation()
    }
}
