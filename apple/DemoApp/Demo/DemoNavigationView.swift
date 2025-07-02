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
    @ViewBuilder var label: some View {
        switch self {
        case .simulated:
            Label("Sim", systemImage: "location.slash.circle")
        case .device:
            Label("Device", systemImage: "location.circle")
        }
    }
}

private extension DemoModel {
    @MainActor func selectRoute(from routes: [Route]) async {
        do {
            guard let route = routes.first else { throw DemoError.noFirstRoute }
            selectedRoute = route
            await chooseRoute(route)
        } catch {
            errorMessage = error.localizedDescription
            appState = .idle
        }
    }
}

private extension DemoAppState {
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

    var body: some View {
        let locationServicesEnabled = model.locationServicesEnabled

        DynamicallyOrientingNavigationView(
            styleURL: AppDefaults.mapStyleURL,
            camera: $model.camera,
            navigationState: model.coreState,
            isMuted: model.core.spokenInstructionObserver.isMuted,
            onTapMute: model.core.spokenInstructionObserver.toggleMute,
            onTapExit: {
                Task {
                    await stopNavigation()
                }
            },
            makeMapContent: {
                let source = ShapeSource(identifier: "userLocation") {
                    // Demonstrate how to add a dynamic overlay;
                    // also incidentally shows the extent of puck lag
                    if let coordinate = model.lastCoordinate {
                        MLNPointFeature(coordinate: coordinate)
                    }
                }
                CircleStyleLayer(identifier: "foo", source: source)
            }
        )
        .navigationSpeedLimit(
            // Configure speed limit signage based on user preference or location
            speedLimit: model.core.annotation?.speedLimit,
            speedLimitStyle: .mutcdStyle
        )
        .innerGrid(
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
            },
            bottomTrailing: {
                VStack {
                    Text(locationLabel)
                        .font(.caption)
                        .padding(.all, 8)
                        .foregroundColor(.white)
                        .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))

                    if locationServicesEnabled {
                        if model.appState.showStateButton {
                            NavigationUIButton {
                                Task {
                                    switch model.appState {
                                    case .idle:
                                        await model.chooseDestination()
                                    case let .destination(coordinate):
                                        isFetchingRoutes = true
                                        await model.loadRoute(coordinate)
                                        isFetchingRoutes = false
                                    case let .routes(routes):
                                        await model.selectRoute(from: routes)
                                    case let .selectedRoute(route):
                                        await startNavigation(route)
                                    case .navigating:
                                        // Should not reach this.
                                        break
                                    }
                                }
                            } label: {
                                Text(model.appState.buttonText)
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.5)
                                    .font(.body.bold())
                            }
                        }
                    } else {
                        NavigationUIButton {
                            // TODO: enable location services.
                        } label: {
                            Text("Enable Location Services")
                        }
                    }
                    Button {
                        model.toggleLocationSimulation()
                    } label: {
                        model.locationProvider.type.label
                    }
                    .buttonStyle(NavigationUIButtonStyle())
                }
            }
        )
    }

    // MARK: Conveniences

    func startNavigation(_ route: Route) async {
        await model.navigate(route)
        preventAutoLock()
    }

    func stopNavigation() async {
        await model.stop()
        allowAutoLock()
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
