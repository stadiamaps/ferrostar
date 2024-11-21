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
    @State private var routes: [Route]?
    @State private var errorMessage: String? {
        didSet {
            Task {
                try await Task.sleep(nanoseconds: 10 * NSEC_PER_SEC)
                errorMessage = nil
            }
        }
    }

    @State private var camera: MapViewCamera = .center(AppDefaults.initialLocation.coordinate, zoom: 14)
    
    var body: some View {
        let locationServicesEnabled = appEnvironment.locationProvider.authorizationStatus == .authorizedAlways
        || appEnvironment.locationProvider.authorizationStatus == .authorizedWhenInUse

        NavigationStack {
            DynamicallyOrientingNavigationView(
                styleURL: AppDefaults.mapStyleURL,
                camera: $camera,
                navigationState: appEnvironment.ferrostarCore.state,
                isMuted: appEnvironment.spokenInstructionObserver.isMuted,
                onTapMute: appEnvironment.spokenInstructionObserver.toggleMute,
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
                            if ferrostarCore.state == nil {
                                NavigationUIButton {
                                    Task {
                                        do {
                                            isFetchingRoutes = true
                                            try startNavigation()
                                            isFetchingRoutes = false
                                        } catch {
                                            isFetchingRoutes = false
                                            errorMessage = "\(error.localizedDescription)"
                                        }
                                    }
                                } label: {
                                    Text("Start Nav")
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.5)
                                        .font(.body.bold())
                                }
                                .disabled(routes?.isEmpty == true)
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
                await getRoutes()
            }
        }
    }

    // MARK: Conveniences

    func getRoutes() async {
        do {
            self.routes = try await appEnvironment.getRoutes()
        } catch {
            print("DemoApp: error getting routes: \(error)")
        }
    }
    
    func startNavigation() throws {
        guard let route = routes?.first else {
            print("DemoApp: No route")
            return
        }

        try appEnvironment.startNavigation(route: route)
        preventAutoLock()
    }

    func stopNavigation() {
        appEnvironment.stopNavigation()
        camera = .center(AppDefaults.initialLocation.coordinate, zoom: 14)
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
