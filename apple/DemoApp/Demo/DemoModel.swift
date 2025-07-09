import Combine
import CoreLocation
@preconcurrency import FerrostarCore
@preconcurrency import FerrostarCoreFFI
import Foundation
import MapKit
import MapLibreSwiftUI

private extension MapViewCamera {
    static func currentLocationCamera(locationProvider: LocationProviding) -> MapViewCamera {
        guard let coordinate = locationProvider.lastLocation?.clLocation.coordinate
        else { return MapViewCamera.default() }
        return .center(coordinate, zoom: 14)
    }
}

private extension CLLocation {
    var simulatedLocationProvider: SimulatedLocationProvider {
        let simulated = SimulatedLocationProvider(location: self)
        simulated.warpFactor = 2
        return simulated
    }
}

private extension FerrostarCore {
    convenience init(locationProvider: LocationProviding) throws {
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

        try self.init(
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
            annotation: AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM()
        )
    }
}

extension DemoModel {
    convenience init?() {
        self.init(locationProvider: SwitchableLocationProvider(
            simulated: AppDefaults.initialLocation.simulatedLocationProvider,
            type: .simulated
        ))

        let originCoordinate = (locationProvider.lastLocation != nil) ? locationProvider.lastLocation!.clLocation
            .coordinate : AppDefaults.initialLocation.coordinate
        origin = MKMapItem(coordinate: originCoordinate)

        // "Cupertino HS"
        destination = MKMapItem(coordinate: CLLocationCoordinate2D(latitude: 37.31910, longitude: -122.01018))
    }
}

@MainActor let demoModel = DemoModel()

extension MKMapItem {
    static var invalid: MKMapItem {
        MKMapItem(coordinate: kCLLocationCoordinate2DInvalid)
    }

    convenience init(coordinate: CLLocationCoordinate2D) {
        self.init(placemark: MKPlacemark(coordinate: coordinate))
    }
}

@MainActor
@Observable final class DemoModel {
    var errorMessage: String?
    var appState: DemoAppState = .idle
    let locationProvider: SwitchableLocationProvider
    let core: FerrostarCore
    var origin: MKMapItem = .invalid
    var destination: MKMapItem = .invalid
    var selectedRoute: Route?
    var camera: MapViewCamera

    var coreRoute: Route?
    var coreState: NavigationState?

    @ObservationIgnored
    private var cancellables = Set<AnyCancellable>()

    private let navigationDelegate = NavigationDelegate()

    init?(
        locationProvider: SwitchableLocationProvider
    ) {
        self.locationProvider = locationProvider
        camera = MapViewCamera.currentLocationCamera(locationProvider: locationProvider)
        do {
            core = try FerrostarCore(locationProvider: locationProvider)
            core.delegate = navigationDelegate

            // Listen to these Publishers in FerrostarCore, and assign to Observable properties.
            core.$state.receive(on: DispatchQueue.main).sink { [weak self] state in
                self?.coreState = state
            }.store(in: &cancellables)

            core.$route.receive(on: DispatchQueue.main).sink { [weak self] route in
                self?.coreRoute = route
            }.store(in: &cancellables)
        } catch {
            return nil
        }
    }

    var locationServicesEnabled: Bool { locationProvider.locationServicesEnabled }
    var lastCoordinate: CLLocationCoordinate2D? { locationProvider.lastLocation?.clLocation.coordinate }
    var horizontalAccuracy: Double? { locationProvider.lastLocation?.horizontalAccuracy }

    private func routes(from: MKMapItem, to: MKMapItem) async throws -> [Route] {
        guard from != .invalid else { throw DemoError.invalidOrigin }
        let originCoordinate = from.placemark.coordinate
        let destinationCoordinate = to.placemark.coordinate

        async let routes = try await core.getRoutes(
            initialLocation: UserLocation(clCoordinateLocation2D: originCoordinate),
            waypoints: [Waypoint(coordinate: GeographicCoordinate(cl: destinationCoordinate), kind: .break)]
        )
        return try await routes
    }

    private func startNavigation(on route: Route) throws -> DemoAppState {
        try locationProvider.use(route: route)
        try core.startNavigation(route: route)
        camera = .automotiveNavigation()
        return .navigating
    }

    private func stopNavigation() -> DemoAppState {
        core.stopNavigation()
        camera = MapViewCamera.currentLocationCamera(locationProvider: locationProvider)
        return .idle
    }

    private func wrap(wrap: () throws -> DemoAppState) {
        do {
            errorMessage = nil
            appState = try wrap()
        } catch {
            errorMessage = error.localizedDescription
            appState = .idle
        }
    }

    private func wrap(wrap: () async throws -> DemoAppState) async {
        do {
            errorMessage = nil
            appState = try await wrap()
        } catch {
            errorMessage = error.localizedDescription
            appState = .idle
        }
    }

    func chooseDestination() {
        wrap {
            try appState.setDestination(destination)
        }
    }

    func loadRoute(_ destination: MKMapItem) async {
        await wrap {
            guard let lastCoordinate else { throw DemoError.noOrigin }
            origin = .init(coordinate: lastCoordinate)
            let routes = try await routes(from: origin, to: destination)
            guard !routes.isEmpty else { throw DemoError.noRoutesLoaded }
            return .routes(routes)
        }
    }

    func chooseRoute(_ route: Route) {
        wrap {
            selectedRoute = route
            return .selectedRoute(route)
        }
    }

    func navigate(_ route: Route) {
        wrap { try startNavigation(on: route) }
    }

    func stop() {
        wrap {
            selectedRoute = nil
            return stopNavigation()
        }
    }

    func toggleLocationSimulation() {
        locationProvider.toggle()
    }
}
