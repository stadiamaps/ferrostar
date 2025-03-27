import CoreLocation
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

/// This is a shared core where ferrostar lives
class AppEnvironment: ObservableObject {
    var locationProvider: LocationProviding
    @Published var ferrostarCore: FerrostarCore
    @Published var spokenInstructionObserver: SpokenInstructionObserver

    let navigationDelegate = NavigationDelegate()

    init(initialLocation: CLLocation = AppDefaults.initialLocation) {
        let simulated = SimulatedLocationProvider(location: initialLocation)
        simulated.warpFactor = 2
        locationProvider = simulated

        // Set up the a standard Apple AV Speech Synth.
        spokenInstructionObserver = .initAVSpeechSynthesizer()

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

        ferrostarCore = try! FerrostarCore(
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

        // NOTE: Not all applications will need a delegate. Read the NavigationDelegate documentation for details.
        ferrostarCore.delegate = navigationDelegate

        // Initialize text-to-speech; note that this is NOT automatic.
        // You must set a spokenInstructionObserver.
        // Fortunately, this is pretty easy with the provided class
        // backed by AVSpeechSynthesizer.
        // You can customize the instance it further as needed,
        // or replace with your own.
        ferrostarCore.spokenInstructionObserver = spokenInstructionObserver
    }

    func getRoutes() async throws -> [Route] {
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

        print("DemoApp: successfully fetched routes")

        if let simulated = locationProvider as? SimulatedLocationProvider {
            // This configures the simulator to the desired route.
            // The ferrostarCore.startNavigation will still start the location
            // provider/simulator.
            simulated
                .lastLocation = UserLocation(clCoordinateLocation2D: route.geometry.first!.clLocationCoordinate2D)
            print("DemoApp: setting initial location")
        }

        return routes
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
