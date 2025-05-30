import CoreLocation
import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

struct DemoNavigationView: View {
    @EnvironmentObject private var appEnvironment: AppEnvironment
    @EnvironmentObject private var ferrostarCore: FerrostarCore

    @State private var isFetchingRoutes = false
    @State private var route: Route?
    @State private var errorMessage: String? {
        didSet {
            Task {
                try await Task.sleep(nanoseconds: 10 * NSEC_PER_SEC)
                errorMessage = nil
            }
        }
    }

    var body: some View {
        let locationServicesEnabled = appEnvironment.locationProvider.authorizationStatus == .authorizedAlways
            || appEnvironment.locationProvider.authorizationStatus == .authorizedWhenInUse

        NavigationStack {
            DynamicallyOrientingNavigationView(
                styleURL: AppDefaults.mapStyleURL,
                camera: $appEnvironment.camera.camera,
                navigationState: appEnvironment.ferrostarCore.state,
                isMuted: appEnvironment.ferrostarCore.spokenInstructionObserver.isMuted,
                onTapMute: appEnvironment.ferrostarCore.spokenInstructionObserver.toggleMute,
                onTapExit: { stopNavigation() },
                makeMapContent: {
                    let source = ShapeSource(identifier: "userLocation") {
                        // Demonstrate how to add a dynamic overlay;
                        // also incidentally shows the extent of puck lag
                        if let userLocation = appEnvironment.locationProvider.lastLocation {
                            MLNPointFeature(coordinate: userLocation.clLocation.coordinate)
                        }
                    }
                    CircleStyleLayer(identifier: "foo", source: source)
                }
            )
            .navigationSpeedLimit(
                // Configure speed limit signage based on user preference or location
                speedLimit: ferrostarCore.annotation?.speedLimit,
                speedLimitStyle: .mutcdStyle
            )
            .innerGrid(
                topCenter: {
                    if let errorMessage {
                        NavigationUIBanner(severity: .error) {
                            Text(errorMessage)
                        }
                        .onTapGesture {
                            self.errorMessage = nil
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
                            if ferrostarCore.state == nil, let route {
                                NavigationUIButton {
                                    Task {
                                        do {
                                            isFetchingRoutes = true
                                            try startNavigation(route)
                                            isFetchingRoutes = false
                                        } catch {
                                            isFetchingRoutes = false
                                            errorMessage = error.localizedDescription
                                        }
                                    }
                                } label: {
                                    Text("Start Nav")
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
            .task {
                do {
                    try await getRoute()
                } catch {
                    errorMessage = error.localizedDescription
                }
            }
        }
    }

    // MARK: Conveniences

    private func getRoute() async throws {
        route = try await appEnvironment.getRoute()
    }

    private func startNavigation(_ route: Route) throws {
        try appEnvironment.startNavigation(route: route)
        appEnvironment.camera.camera = .automotiveNavigation()
        preventAutoLock()
    }

    func stopNavigation() {
        appEnvironment.stopNavigation()
        appEnvironment.camera.camera = .center(AppDefaults.initialLocation.coordinate, zoom: 14)
        allowAutoLock()
    }

    var locationLabel: String {
        guard let userLocation = appEnvironment.locationProvider.lastLocation else {
            return "No location - authed as \(appEnvironment.locationProvider.authorizationStatus)"
        }

        return "±\(Int(userLocation.horizontalAccuracy))m accuracy"
    }

    private func preventAutoLock() {
        UIApplication.shared.isIdleTimerDisabled = true
    }

    private func allowAutoLock() {
        UIApplication.shared.isIdleTimerDisabled = false
    }
}

#Preview {
    DemoNavigationView()
}
