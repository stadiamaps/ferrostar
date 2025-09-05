import Combine
import CoreLocation
@preconcurrency import FerrostarCore
@preconcurrency import FerrostarCoreFFI
import FerrostarSwiftUI
import Foundation
import MapLibreSwiftUI
import OSLog
import StadiaMaps

private let logger = Logger(subsystem: "Ferrostar Demo", category: "DemoModel")

@MainActor
@Observable final class DemoModel {
    var errorMessage: String?
    var appState: DemoAppState = .idle

    @ObservationIgnored
    private var spokenInstructionsObserver: SpokenInstructionObserver {
        ferrostarCore.spokenInstructionObserver
    }

    @ObservationIgnored
    private var ferrostarCore: FerrostarCore {
        AppService.shared.ferrostarCore
    }

    @ObservationIgnored
    private var locationProvider: SwitchableLocationProvider {
        AppService.shared.locationProvider
    }

    @ObservationIgnored
    private var appService: AppService {
        AppService.shared
    }

    var lastLocation: CLLocation? {
        guard let lastCoordinate else { return nil }
        return CLLocation(latitude: lastCoordinate.latitude, longitude: lastCoordinate.longitude)
    }

    var camera: MapViewCamera
    var locationType: SwitchableLocationProvider.State = .simulated

    var coreRoute: Route?
    var coreState: NavigationState?
    var speedLimit: Measurement<UnitSpeed>?
    var isMuted: Bool = false

    @ObservationIgnored
    private var cancellables = Set<AnyCancellable>()

    init() {
        let locationProvider = AppService.shared.locationProvider

        camera = MapViewCamera.currentLocationCamera(locationProvider: locationProvider)

        appService.$appState
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in
                self?.appState = newState
                self?.applyAppState()
            }
            .store(in: &cancellables)

        // Listen to these Publishers in FerrostarCore, and assign to Observable properties.
        ferrostarCore.$state.receive(on: DispatchQueue.main).sink { [weak self] state in
            self?.coreState = state
            self?.speedLimit = self?.ferrostarCore.annotation?.speedLimit
        }.store(in: &cancellables)

        ferrostarCore.$route.receive(on: DispatchQueue.main).sink { [weak self] route in
            self?.coreRoute = route
        }.store(in: &cancellables)

        ferrostarCore.spokenInstructionObserver.$isMuted.sink { [weak self] isMuted in
            self?.isMuted = isMuted
        }.store(in: &cancellables)

        locationProvider.$type.sink { [weak self] locationType in
            self?.locationType = locationType
        }.store(in: &cancellables)
    }

    var locationServicesEnabled: Bool { locationProvider.locationServicesEnabled }
    var lastCoordinate: CLLocationCoordinate2D? { locationProvider.lastLocation?.clLocation.coordinate }
    var horizontalAccuracy: Double? { locationProvider.lastLocation?.horizontalAccuracy }

    private func startNavigation(on route: Route) throws {
        try locationProvider.use(route: route)
        try ferrostarCore.startNavigation(route: route)
        camera = .automotiveNavigation()
    }

    private func stopNavigation() {
        ferrostarCore.stopNavigation()
        camera = MapViewCamera.currentLocationCamera(locationProvider: locationProvider)
    }

    func applyAppState() {
        switch appState {
        case .idle:
            break
        case let .routes(routes: routes):
            if let boundingBox = routes.boundingBox {
                camera = .boundingBox(boundingBox)
            }
        case .selectedRoute:
            break
        case .navigating:
            break
        }
    }

    func handleError(_ error: Error, newAppState: DemoAppState = .idle) {
        logger.error("\(error)")
        errorMessage = error.localizedDescription
        appState = newAppState
    }

    func toggleMute() {
        ferrostarCore.spokenInstructionObserver.toggleMute()
    }

    private func wrap(wrap: () throws -> Void) {
        do {
            errorMessage = nil
            try wrap()
        } catch {
            handleError(error)
        }
    }

    private func wrap(wrap: () async throws -> Void) async {
        do {
            errorMessage = nil
            try await wrap()
        } catch {
            handleError(error)
        }
    }

    func updateDestination(to point: Point) {
        Task {
            await wrap {
                guard let lastLocation else { throw DemoError.noOrigin }

                try await AppService.shared.fetchRoutes(
                    origin: lastLocation,
                    destination: point.coordinate
                )
            }
        }
    }

    func chooseRoute(_ route: Route) {
        wrap {
            appService.selectRoute(route)
        }
    }

    func navigate(_ route: Route) {
        wrap { try startNavigation(on: route) }
    }

    func stop() {
        wrap {
            camera = .trackUserLocation()
            return stopNavigation()
        }
    }

    func toggleLocationSimulation() {
        locationProvider.toggle()
    }
}
