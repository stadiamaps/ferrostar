import Combine
import CoreLocation
import FerrostarCore
import FerrostarCoreFFI
import Foundation
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

extension DemoModel {
    convenience init?() {
        self.init(locationProvider: SwitchableLocationProvider(
            simulated: AppDefaults.initialLocation.simulatedLocationProvider,
            type: .simulated
        ))
    }
}

let demoModel = DemoModel()

@Observable final class DemoModel {
    @ObservationIgnored
    let locationProvider: SwitchableLocationProvider

    @ObservationIgnored
    let core: FerrostarCore

    @ObservationIgnored
    private var cancellables = Set<AnyCancellable>()

    @ObservationIgnored
    private let navigationDelegate = NavigationDelegate()

    @MainActor var errorMessage: String?
    @MainActor var appState: DemoAppState = .idle

    @MainActor var origin: CLLocationCoordinate2D = kCLLocationCoordinate2DInvalid
    @MainActor var destination: CLLocationCoordinate2D = kCLLocationCoordinate2DInvalid
    @MainActor var selectedRoute: Route?
    @MainActor var camera: MapViewCamera = .default()

    @MainActor var coreRoute: Route?
    @MainActor var coreState: NavigationState?

    init?(
        locationProvider: SwitchableLocationProvider
    ) {
        self.locationProvider = locationProvider

        do {
            core = try FerrostarCore(locationProvider: locationProvider)
            core.delegate = navigationDelegate

            // Listen to these Publishers in FerrostarCore, and assign to Observable properties.
            core.$state.receive(on: DispatchQueue.main).sink { [weak self] state in
                Task {
                    await MainActor.run { [weak self] in
                        self?.coreState = state
                    }
                }
            }.store(in: &cancellables)

            core.$route.receive(on: DispatchQueue.main).sink { [weak self] route in
                Task {
                    await MainActor.run { [weak self] in
                        self?.coreRoute = route
                    }
                }
            }.store(in: &cancellables)
        } catch {
            return nil
        }
    }

    func onAppear() {
        Task {
            await MainActor.run {
                camera = MapViewCamera.currentLocationCamera(locationProvider: locationProvider)
            }
        }
    }

    var locationServicesEnabled: Bool { locationProvider.locationServicesEnabled }
    var lastCoordinate: CLLocationCoordinate2D? { locationProvider.lastLocation?.clLocation.coordinate }
    var horizontalAccuracy: Double? { locationProvider.lastLocation?.horizontalAccuracy }

    private func routes(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) async throws -> [Route] {
        guard from != kCLLocationCoordinate2DInvalid else { throw DemoError.invalidOrigin }
        return try await core.getRoutes(
            initialLocation: UserLocation(clCoordinateLocation2D: from),
            waypoints: [Waypoint(coordinate: GeographicCoordinate(cl: to), kind: .break)]
        )
    }

    private func startNavigation(on route: Route) async throws -> DemoAppState {
        try locationProvider.use(route: route)
        try core.startNavigation(route: route)
        await MainActor.run {
            camera = .automotiveNavigation()
        }
        return .navigating
    }

    private func stopNavigation() async -> DemoAppState {
        core.stopNavigation()
        await MainActor.run {
            camera = MapViewCamera.currentLocationCamera(locationProvider: locationProvider)
        }
        return .idle
    }

    private func wrap(wrap: () async throws -> DemoAppState) async {
        do {
            await MainActor.run {
                errorMessage = nil
            }
            let newState = try await wrap()
            await MainActor.run {
                appState = newState
            }
        } catch {
            await MainActor.run {
                errorMessage = error.localizedDescription
                appState = .idle
            }
        }
    }

    func chooseDestination() async {
        await wrap {
            await MainActor.run {
                origin = (locationProvider.lastLocation != nil) ? locationProvider.lastLocation!.clLocation
                    .coordinate : AppDefaults.initialLocation.coordinate

                // "Cupertino HS"
                destination = CLLocationCoordinate2D(latitude: 37.31910, longitude: -122.01018)
            }

            return try await appState.setDestination(destination)
        }
    }

    func loadRoute(_ destination: CLLocationCoordinate2D) async {
        await wrap {
            guard let lastCoordinate else { throw DemoError.noOrigin }
            await MainActor.run {
                origin = lastCoordinate
            }
            let routes = try await routes(from: origin, to: destination)
            guard !routes.isEmpty else { throw DemoError.noRoutesLoaded }
            return .routes(routes)
        }
    }

    func chooseRoute(_ route: Route) async {
        await wrap {
            await MainActor.run {
                selectedRoute = route
            }
            return .selectedRoute(route)
        }
    }

    func navigate(_ route: Route) async {
        await wrap { try await startNavigation(on: route) }
    }

    func stop() async {
        await wrap {
            await MainActor.run {
                selectedRoute = nil
            }
            return await stopNavigation()
        }
    }

    func toggleLocationSimulation() {
        locationProvider.toggle()
    }
}
