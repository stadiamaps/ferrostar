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

struct DemoNavigationView: View {
    @State var model = DemoModel()
    @State private var isFetchingRoutes = false

    var body: some View {
        VStack {
            DynamicallyOrientingNavigationView(
                styleURL: AppDefaults.mapStyleURL,
                camera: $model.camera,
                navigationState: model.coreState,
                isMuted: model.isMuted,
                onTapMute: model.toggleMute,
                onTapExit: { model.stop() },
                makeMapContent: {
                    if case let .routes(routes: routes) = model.appState {
                        for (idx, route) in routes.enumerated() {
                            RouteStyleLayer(polyline: route.polyline, identifier: "route-\(idx)")
                        }
                    }

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
                speedLimit: model.speedLimit,
                speedLimitStyle: .mutcdStyle
            )
            .innerGrid(
                bottomLeading: {
                    VStack {
                        Text(locationLabel)
                            .font(.caption)
                            .padding(.all, 8)
                            .foregroundColor(.white)
                            .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))

                        Button {
                            model.toggleLocationSimulation()
                        } label: {
                            model.locationType.label
                        }
                        .buttonStyle(NavigationUIButtonStyle())
                    }
                },
                bottomTrailing: {
                    VStack {
                        if model.locationServicesEnabled {
                            if let buttonText = model.appState.buttonText {
                                NavigationUIButton {
                                    switch model.appState {
                                    case let .routes(routes):
                                        // TODO: Revise this to only work with 1 route returned once/if route selection is added to demo.
                                        if let route = routes.first {
                                            startNavigation(route)
                                            return
                                        }

//                                        model.selectRoute(from: routes)
                                    case let .selectedRoute(route):
                                        startNavigation(route)

                                    case .idle, .navigating:
                                        // Should not reach this.
                                        break
                                    }
                                } label: {
                                    Text(buttonText)
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
                    }
                }
            )

            if model.appState == .idle {
                SearchSheet(userLocation: model.lastLocation) { point in
                    model.updateDestination(to: point)
                }
                .frame(height: 220)
            }
        }
        .overlay {
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
