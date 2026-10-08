import CoreLocation
import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

private extension SwitchableLocationProvider.State {
    var explanation: String {
        switch self {
        case .simulated:
            "A fixed start location, then follows the route while navigating."
        case .device:
            "From the device (or the Simulator's location setting)."
        }
    }
}

private extension DemoModel {
    func selectRoute(from routes: [Route]) {
        do {
            guard let route = routes.first else { throw DemoError.noFirstRoute }
            selectedRoute = route
            chooseRoute(route)
        } catch {
            handleError(error)
        }
    }
}

private extension DemoAppState {
    var hasDestination: Bool {
        switch self {
        case .idle:
            false
        case .destination(_), .routes(_), .selectedRoute, .navigating:
            true
        }
    }

    var previewRoutes: [Route] {
        switch self {
        case let .routes(routes):
            routes
        case let .selectedRoute(route):
            [route]
        case .idle, .destination(_), .navigating:
            []
        }
    }

    var showStateButton: Bool {
        switch self {
        case .idle, .destination(_), .routes(_), .selectedRoute:
            true
        case .navigating:
            false
        }
    }
}

struct DemoNavigationView: View {
    @Bindable var model: DemoModel
    @State private var isFetchingRoutes = false
    @State private var showSearch = false

    var body: some View {
        let locationServicesEnabled = model.locationServicesEnabled

        VStack {
            DynamicallyOrientingNavigationView(
                styleURL: AppDefaults.mapStyleURL,
                camera: $model.camera,
                navigationState: model.coreState,
                isMuted: model.core.spokenInstructionObserver.isMuted,
                onTapMute: model.core.spokenInstructionObserver.toggleMute,
                onTapExit: { stopNavigation() },
                makeMapContent: {
                    let source = ShapeSource(identifier: "userLocation") {
                        // Demonstrate how to add a dynamic overlay;
                        // also incidentally shows the extent of puck lag
                        if let coordinate = model.lastCoordinate {
                            MLNPointFeature(coordinate: coordinate)
                        }
                    }
                    CircleStyleLayer(identifier: "foo", source: source)

                    // Ferrostar draws the route itself once navigating.
                    for (index, route) in model.appState.previewRoutes.enumerated() {
                        RouteStyleLayer(
                            polyline: MLNPolylineFeature(coordinates: route.geometry.map(\.clLocationCoordinate2D)),
                            identifier: "preview-route-\(index)",
                            style: index == 0 ? ActiveRouteStyle() : TravelledRouteStyle()
                        )
                    }

                    if model.appState.hasDestination {
                        let destination = ShapeSource(identifier: "destination") {
                            MLNPointFeature(coordinate: model.destination)
                        }
                        CircleStyleLayer(identifier: "destination-pin", source: destination)
                            .radius(8)
                            .color(.systemRed)
                            .strokeWidth(2)
                            .strokeColor(.white)
                    }
                }
            )
            .unsafeMapViewControllerModifier { controller in
                // While idle, MapLibre's puck shows the device's location, which only CoreLocation reports.
                controller.mapView.showsUserLocation =
                    model.locationProvider.type == .device || model.coreState?.isNavigating == true
            }
            .navigationSpeedLimit(
                // Configure speed limit signage based on user preference or location
                speedLimit: model.core.annotation?.speedLimit,
                speedLimitStyle: .mutcdStyle
            )
            .navigationViewInnerGrid(
                topCenter: {
                    if let errorMessage = model.errorMessage {
                        NavigationUIBanner(severity: .error) {
                            Text(errorMessage)
                        }
                        .onTapGesture {
                            model.errorMessage = nil
                        }
                    } else if isFetchingRoutes {
                        NavigationUIBanner(severity: .loading) {
                            Text("Loading route...")
                        }
                    }
                }
            )
            .overlay(alignment: .topTrailing) {
                if model.appState.showStateButton {
                    locationSourcePicker
                        .padding(.trailing, 16)
                        .padding(.top, 16)
                }
            }
            .overlay(alignment: .bottomTrailing) {
                if model.appState.showStateButton {
                    browseControls(locationServicesEnabled: locationServicesEnabled)
                        .padding(.trailing, 16)
                        .padding(.bottom, 16)
                }
            }

            if showSearch {
                model.searchView { showSearch = false }
            }
        }
    }

    // MARK: Conveniences

    func startNavigation(_ route: Route) {
        model.navigate(route)
        preventAutoLock()
    }

    func stopNavigation() {
        model.stop()
        allowAutoLock()
    }

    func browseControls(locationServicesEnabled: Bool) -> some View {
        VStack {
            Text(locationLabel)
                .font(.caption)
                .padding(.all, 8)
                .foregroundColor(.white)
                .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))

            if locationServicesEnabled {
                NavigationUIButton {
                    switch model.appState {
                    case .idle:
                        showSearch = true
                    case let .destination(coordinate):
                        Task {
                            isFetchingRoutes = true
                            await model.loadRoute(coordinate)
                            isFetchingRoutes = false
                        }
                    case let .routes(routes):
                        model.selectRoute(from: routes)
                    case let .selectedRoute(route):
                        startNavigation(route)
                    case .navigating:
                        // Should not reach this.
                        break
                    }
                } label: {
                    Text(model.appState.buttonText)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .font(.body.bold())
                }
            } else {
                NavigationUIButton {
                    // TODO: enable location services.
                } label: {
                    Text("Enable Location Services")
                }
            }
        }
    }

    var locationSourcePicker: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Location Source")
                .font(.caption.bold())
            Picker("Location Source", selection: Binding(
                get: { model.locationProvider.type },
                set: {
                    if $0 != model.locationProvider.type {
                        model.toggleLocationSimulation()
                    }
                }
            )) {
                Text("Simulated").tag(SwitchableLocationProvider.State.simulated)
                Text("CoreLocation").tag(SwitchableLocationProvider.State.device)
            }
            .pickerStyle(.segmented)
            Text(model.locationProvider.type.explanation)
                .font(.caption)
        }
        .frame(width: 220)
        .padding(8)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8))
    }

    var locationLabel: String {
        guard let horizontalAccuracy = model.horizontalAccuracy else {
            return "Not Authorized"
        }

        return "±\(Int(horizontalAccuracy))m accuracy"
    }

    private func preventAutoLock() {
        UIApplication.shared.isIdleTimerDisabled = true
    }

    private func allowAutoLock() {
        UIApplication.shared.isIdleTimerDisabled = false
    }
}
