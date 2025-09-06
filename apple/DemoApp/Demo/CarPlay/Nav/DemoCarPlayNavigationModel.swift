import CarPlay
import Combine
import FerrostarCarPlayUI
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import MapLibreSwiftUI
import OSLog
import SwiftUI

private let logger = Logger(subsystem: "Rallista", category: "CarPlayNavViewModel")

@MainActor
@Observable
class DemoCarPlayNavigationModel {
    @ObservationIgnored
    var mapTemplate: CPMapTemplate?
    @ObservationIgnored
    var navigationAlertTemplate: CPNavigationAlert?
    @ObservationIgnored
    weak var navController: DemoCarPlayNavController?

    @ObservationIgnored
    private let formatterCollection: FormatterCollection = FoundationFormatterCollection()

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

    var appState: DemoAppState = .idle
    var lastCoordinate: CLLocationCoordinate2D?
    var navigationState: NavigationState?
    var route: Route?
    var isMuted: Bool = false
    var errorMessage: String?
    var camera: MapViewCamera = .automotiveNavigation()

    var speedLimit: Measurement<UnitSpeed>?

    @ObservationIgnored
    let showcasePadding = UIEdgeInsets(top: 32, left: 64, bottom: 32, right: 64)

    private var cancellables = Set<AnyCancellable>()

    init() {
        camera = MapViewCamera.currentLocationCamera(locationProvider: locationProvider)

        appService.$appState
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in
                self?.appState = newState
                self?.applyAppState()
            }
            .store(in: &cancellables)

        ferrostarCore.$state
            .receive(on: DispatchQueue.main)
            .sink { [weak self] state in
                guard let self else { return }
                navigationState = state
                updateTemplate()
            }
            .store(in: &cancellables)

        ferrostarCore.annotation?.speedLimit.publisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newSpeedLimit in
                guard let self else { return }
                speedLimit = newSpeedLimit
            }
            .store(in: &cancellables)

        spokenInstructionsObserver.$isMuted
            .receive(on: DispatchQueue.main)
            .sink { [weak self] value in
                guard let self else { return }
                isMuted = value
            }
            .store(in: &cancellables)
    }

    func onAppear(_ navController: DemoCarPlayNavController) async throws {
        self.navController = navController

        let mapTemplate = CPMapTemplate()
        mapTemplate.automaticallyHidesNavigationBar = false

        self.mapTemplate = mapTemplate

        do {
            try await CarPlaySession.shared.setRootTemplate(mapTemplate)
            updateTemplate()
        } catch {
            logger.error("error setting up template")
        }

        Task { [weak self] in
            guard let self else { return }

            // This starts a new navigaiton session anytime a new route is attached to ferrostar core.
            for await newRoute in ferrostarCore.$route.values {
                guard let newRoute else {
                    continue
                }

                startSession(for: newRoute)
                route = newRoute
            }
        }
    }

    func startSession(for route: FerrostarCoreFFI.Route) {
        guard let mapTemplate else {
            return
        }

        Task {
            do {
                let trip = try CPTrip.fromFerrostar(
                    routes: [route],
                    distanceFormatter: formatterCollection.distanceFormatter,
                    durationFormatter: formatterCollection.durationFormatter
                )

                try CarPlaySession.shared.startNavigationSession(on: mapTemplate, trip: trip)
            } catch {
                logger.error("error starting session \(error)")
            }
        }
    }

    func completeTrip() {}

    func updateTemplate() {
        guard let mapTemplate else {
            return
        }

        if let session = CarPlaySession.shared.session {
            navigationState?.updateEstimates(
                mapTemplate: mapTemplate,
                session: session,
                units: .default
            )

            mapTemplate.leadingNavigationBarButtons = []
            mapTemplate.trailingNavigationBarButtons = [
                NavigationBarButtons.stop { [weak self] in
                    self?.stopNavigation()
                },
            ]

        } else {
            //            mapTemplate?.present(navigationAlert: .refresh {
            //                <#code#>
            //            }, animated: true)

            mapTemplate.leadingNavigationBarButtons = [
                NavigationBarButtons.search { [weak self] in
                    self?.navController?.navigate(to: .search)
                },
            ]
            mapTemplate.trailingNavigationBarButtons = []
        }

        var centeringButton: CPMapButton? = nil
        if case .trackingUserLocationWithCourse = camera.state {
            centeringButton = MapButtons.centerOn(true, action: showcaseRoute)
        } else {
            centeringButton = MapButtons.centerOn(false, action: trackUser)
        }

        mapTemplate.mapButtons = [
            MapButtons.toggleMute(isMuted, action: toggleMute),
            MapButtons.zoomIn(action: zoomIn),
            MapButtons.zoomOut(action: zoomOut),
            centeringButton,
        ].compactMap { $0 }
    }

    func clearError() {
        //        state.routeStatus = .ok
    }

    func stopNavigation() {
        camera = .trackUserLocation()
        AppService.shared.stopNavigation()
        CarPlaySession.shared.cancelNavigationSession()
    }

    func toggleMute() {
        spokenInstructionsObserver.toggleMute()
        updateTemplate()
    }

    func trackUser() {
        camera = .automotiveNavigation(zoom: 15)
        updateTemplate()
    }

    func showcaseRoute() {
        guard let routeOverviewCamera = navigationState?.routeOverviewCamera else {
            updateTemplate()
            return
        }

        camera = routeOverviewCamera
        updateTemplate()
    }

    func zoomIn() {
        camera.incrementZoom(by: 1)
    }

    func zoomOut() {
        camera.incrementZoom(by: -1)
    }

    func applyAppState() {
        switch appState {
        case .idle:
            break
        case let .routes(routes: routes):
            if let boundingBox = routes.boundingBox {
                camera = .boundingBox(boundingBox, edgePadding: showcasePadding)
            }
        case .selectedRoute:
            break
        case .navigating:
            break
        }
    }
}
