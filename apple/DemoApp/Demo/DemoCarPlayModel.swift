@preconcurrency import CarPlay
import FerrostarCarPlayUI
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import Foundation
import MapLibreSwiftUI

@MainActor
private extension CPBarButton {
    convenience init(appState: DemoAppState, model: DemoCarPlayModel, mapTemplate: CPMapTemplate) {
        self.init(title: appState.buttonText, handler: appState.handler(model, mapTemplate: mapTemplate))
    }

    convenience init(model: DemoCarPlayModel, mapTemplate: CPMapTemplate) {
        let appState = model.appState
        self.init(appState: appState, model: model, mapTemplate: mapTemplate)
    }
}

private extension DemoAppState {
    @MainActor
    func handler(_ model: DemoCarPlayModel, mapTemplate: CPMapTemplate) -> CPBarButtonHandler {
        { _ in
            switch self {
            case .idle:
                model.chooseDestination(mapTemplate)
            case let .destination(item):
                Task {
                    await model.loadRoute(item, mapTemplate)
                }
            case let .routes(routes):
                model.preview(routes, mapTemplate: mapTemplate)
            case let .selectedRoute(route):
                model.startNavigationSession(route, mapTemplate: mapTemplate)
            case .navigating:
                model.stop(cancelTrip: false, mapTemplate: mapTemplate)
            }
        }
    }

    var leadingNavigationBarButtonsEmpty: Bool {
        switch self {
        case .idle, .destination, .routes:
            false
        case .selectedRoute, .navigating:
            true
        }
    }

    var trailingNavigationBarButtonsEmpty: Bool {
        switch self {
        case .idle:
            true
        case .destination, .routes, .selectedRoute, .navigating:
            false
        }
    }
}

@MainActor
@Observable final class DemoCarPlayModel: NSObject, @preconcurrency CPMapTemplateDelegate {
    private var model: DemoModel
    private var session: CPNavigationSession?

    private let formatterCollection: FormatterCollection = FoundationFormatterCollection()

    init(model: DemoModel) {
        self.model = model
    }

    func createAndAttachTemplate() -> CPMapTemplate {
        let mapTemplate = CPMapTemplate()
        mapTemplate.mapDelegate = self
        updateTemplate(mapTemplate)
        return mapTemplate
    }

    var appState: DemoAppState { model.appState }
    var errorMessage: String? {
        get {
            model.errorMessage
        }
        set {
            model.errorMessage = newValue
        }
    }

    var coreState: NavigationState? { model.coreState }
    var camera: MapViewCamera {
        get {
            model.camera
        }
        set {
            model.camera = newValue
        }
    }

    func chooseDestination(_ mapTemplate: CPMapTemplate) {
        model.chooseDestination()
        updateTemplate(mapTemplate)
    }

    func loadRoute(_ destination: MKMapItem, _ mapTemplate: CPMapTemplate) async {
        await model.loadRoute(destination)
        updateTemplate(mapTemplate)
    }

    func stop(cancelTrip: Bool, mapTemplate: CPMapTemplate?) {
        if cancelTrip {
            session?.cancelTrip()
        } else {
            session?.finishTrip()
        }
        session = nil
        model.stop()
        if let mapTemplate {
            updateTemplate(mapTemplate)
        }
    }

    private func start(choice: CPRouteChoice, mapTemplate: CPMapTemplate) {
        do {
            guard let route = choice.route else { throw DemoError.invalidCPRouteChoice }
            startNavigationSession(route, mapTemplate: mapTemplate)
        } catch {
            model.errorMessage = error.localizedDescription
            model.appState = .idle
        }
    }

    private func select(choice: CPRouteChoice, mapTemplate: CPMapTemplate) {
        do {
            guard let route = choice.route else { throw DemoError.invalidCPRouteChoice }
            model.chooseRoute(route)
            updateTemplate(mapTemplate)
        } catch {
            model.errorMessage = error.localizedDescription
            model.appState = .idle
        }
    }

    private func observeFerrorstarChanges(_ mapTemplate: CPMapTemplate, units: MKDistanceFormatter.Units = .default) {
        withObservationTracking {
            guard let session else {
                return
            }

            guard let state = coreState else {
                // This occurs when navigation hasn't yet started.
                return
            }

            if case .complete = state.tripState {
                stop(cancelTrip: false, mapTemplate: mapTemplate)
            } else {
                state.updateEstimates(mapTemplate: mapTemplate, session: session, units: units)
            }
        } onChange: {
            Task { @MainActor in
                // Observe again after a change.
                self.observeFerrorstarChanges(mapTemplate, units: units)
            }
        }
    }

    private func trip(_ routes: [Route]) -> CPTrip {
        CPTrip.fromFerrostar(
            routes: routes,
            origin: model.origin,
            destination: model.destination,
            distanceFormatter: formatterCollection.distanceFormatter,
            durationFormatter: formatterCollection.durationFormatter
        )
    }

    func startNavigationSession(_ route: Route, mapTemplate: CPMapTemplate) {
        let trip = trip([route])
        session = mapTemplate.startNavigationSession(for: trip)
        model.navigate(route)
        observeFerrorstarChanges(mapTemplate)
        updateTemplate(mapTemplate)
    }

    func preview(_ routes: [Route], mapTemplate: CPMapTemplate) {
        let trip = trip(routes)
        mapTemplate.showRouteChoicesPreview(for: trip, textConfiguration: nil)
        updateTemplate(mapTemplate)
    }

    private func leadingNavigationBarButtons(_ mapTemplate: CPMapTemplate) -> [CPBarButton] {
        if appState.leadingNavigationBarButtonsEmpty {
            []
        } else {
            [CPBarButton(model: self, mapTemplate: mapTemplate)]
        }
    }

    private func trailingNavigationBarButtons(_ mapTemplate: CPMapTemplate) -> [CPBarButton] {
        if appState.trailingNavigationBarButtonsEmpty {
            []
        } else {
            [CPBarButton(appState: .navigating, model: self, mapTemplate: mapTemplate)]
        }
    }

    private func updateTemplate(_ mapTemplate: CPMapTemplate) {
        mapTemplate.automaticallyHidesNavigationBar = false
        mapTemplate.leadingNavigationBarButtons = leadingNavigationBarButtons(mapTemplate)
        mapTemplate.trailingNavigationBarButtons = trailingNavigationBarButtons(mapTemplate)
        mapTemplate
            .mapButtons = [CarPlayMapButtons.recenterButton { [self] in
                model.camera = .automotiveNavigation(pitch: 25)
            }]
    }

    func mapTemplate(_ mapTemplate: CPMapTemplate, selectedPreviewFor _: CPTrip, using routeChoice: CPRouteChoice) {
        // FIXME: Show route overview on the map.
        select(choice: routeChoice, mapTemplate: mapTemplate)
    }

    func mapTemplate(_ mapTemplate: CPMapTemplate, startedTrip _: CPTrip, using routeChoice: CPRouteChoice) {
        start(choice: routeChoice, mapTemplate: mapTemplate)
        mapTemplate.hideTripPreviews()
    }

    func mapTemplateDidCancelNavigation(_ mapTemplate: CPMapTemplate) {
        stop(cancelTrip: true, mapTemplate: mapTemplate)
    }
}
