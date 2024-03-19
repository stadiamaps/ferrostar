import CoreLocation
import FerrostarCore
import struct FerrostarCoreFFI.GeographicCoordinate
import struct FerrostarCoreFFI.Route
import struct FerrostarCoreFFI.UserLocation
import struct FerrostarCoreFFI.Waypoint
import enum FerrostarCoreFFI.WaypointKind
import FerrostarMapLibreUI
import MapLibreSwiftUI
import SwiftUI

let style = URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(APIKeys.shared.stadiaMapsAPIKey)")!
private let initialLocation = CLLocation(latitude: 37.332726,
                                         longitude: -122.031790)

struct NavigationView: View {
    private let navigationDelegate = NavigationDelegate()

    private var locationProvider: LocationProviding
    @ObservedObject private var ferrostarCore: FerrostarCore

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

    @State private var camera: MapViewCamera = .center(initialLocation.coordinate, zoom: 14)

    init() {
        let simulated = SimulatedLocationProvider(location: initialLocation)
        simulated.warpFactor = 2
        locationProvider = simulated
        ferrostarCore = FerrostarCore(
            valhallaEndpointUrl: URL(
                string: "https://api.stadiamaps.com/route/v1?api_key=\(APIKeys.shared.stadiaMapsAPIKey)"
            )!,
            profile: "pedestrian",
            locationProvider: locationProvider
        )
        // NOTE: Not all applications will need a delegate. Read the NavigationDelegate documentation for details.
        ferrostarCore.delegate = navigationDelegate
    }

    var body: some View {
        let locationServicesEnabled = locationProvider.authorizationStatus == .authorizedAlways
            || locationProvider.authorizationStatus == .authorizedWhenInUse

        NavigationStack {
            NavigationMapView(
                lightStyleURL: style,
                darkStyleURL: style,
                navigationState: ferrostarCore.state,
                camera: $camera
            )
            .overlay(alignment: .bottomLeading) {
                VStack {
                    HStack {
                        if isFetchingRoutes {
                            Text("Loading route...")
                                .font(.caption)
                                .padding(.all, 8)
                                .foregroundColor(.white)
                                .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))
                        }

                        if let errorMessage {
                            Text(errorMessage)
                                .font(.caption)
                                .padding(.all, 8)
                                .foregroundColor(.white)
                                .background(Color.red.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))
                                .onTapGesture {
                                    self.errorMessage = nil
                                }
                        }

                        Spacer()

                        NavigationLink {
                            ConfigurationView()
                        } label: {
                            Image(systemName: "gear")
                        }
                        .padding(.all, 8)
                        .background(
                            Color.white
                                .clipShape(.buttonBorder, style: FillStyle())
                                .shadow(radius: 4)
                        )
                        .padding(.top, 56)
                    }

                    Spacer()

                    HStack {
                        Text(locationLabel)
                            .font(.caption)
                            .padding(.all, 8)
                            .foregroundColor(.white)
                            .background(Color.black.opacity(0.7).clipShape(.buttonBorder, style: FillStyle()))

                        Spacer()

                        if locationServicesEnabled {
                            Button("Start Navigation") {
                                Task {
                                    do {
                                        isFetchingRoutes = true
                                        try await startNavigation()
                                        isFetchingRoutes = false
                                    } catch {
                                        isFetchingRoutes = false
                                        errorMessage = "\(error.localizedDescription)"
                                    }
                                }
                            }
                            .disabled(routes?.isEmpty == true)
                            .padding(.all, 8)
                            .background(
                                Color.white
                                    .clipShape(.buttonBorder, style: FillStyle())
                                    .shadow(radius: 4)
                            )
                        } else {
                            Button("Enable Location Services") {
                                // TODO: enable location services.
                            }
                            .padding(.all, 8)
                            .background(
                                Color.white
                                    .clipShape(.buttonBorder, style: FillStyle())
                                    .shadow(radius: 4)
                            )
                        }
                    }
                }
                .padding()
                .padding(.bottom, 32)
                .task {
                    await getRoutes()
                }
            }
        }
    }

    // MARK: Conveniences

    func getRoutes() async {
        guard let userLocation = locationProvider.lastLocation else {
            print("No user location")
            return
        }

        do {
            let waypoints = locations.map { Waypoint(
                coordinate: GeographicCoordinate(lat: $0.coordinate.latitude, lng: $0.coordinate.longitude),
                kind: .break
            ) }
            routes = try await ferrostarCore.getRoutes(initialLocation: userLocation,
                                                       waypoints: waypoints)

            print("DemoApp: successfully fetched a route")

            if let simulated = locationProvider as? SimulatedLocationProvider, let route = routes?.first {
                // This configures the simulator to the desired route.
                // The ferrostarCore.startNavigation will still start the location
                // provider/simulator.
                simulated
                    .lastLocation = UserLocation(clCoordinateLocation2D: route.geometry.first!.clLocationCoordinate2D)
                print("DemoApp: setting initial location")
            }
        } catch {
            print("DemoApp: error fetching route: \(error)")
            errorMessage = "\(error)"
        }
    }

    func startNavigation() async throws {
        guard let route = routes?.first else {
            print("DemoApp: No route")
            return
        }

        // Configure the navigation session.
        // You have a lot of flexibility here based on your use case
        let config = NavigationControllerConfig(
            stepAdvance: .relativeLineStringDistance(minimumHorizontalAccuracy: 32, automaticAdvanceDistance: 10),
            routeDeviationTracking: .staticThreshold(minimumHorizontalAccuracy: 25, maxAcceptableDeviation: 20)
        )

        if let simulated = locationProvider as? SimulatedLocationProvider {
            // This configures the simulator to the desired route.
            // The ferrostarCore.startNavigation will still start the location
            // provider/simulator.
            try simulated.setSimulatedRoute(route)
            print("DemoApp: setting route to be simulated")
        }

        // Starts the navigation state machine.
        // It's worth having a look through the parameters,
        // as most of the configuration happens here.
        try ferrostarCore.startNavigation(
            route: route,
            config: config
        )
    }

    var locationLabel: String {
        guard let userLocation = locationProvider.lastLocation else {
            return "No location - authed as \(locationProvider.authorizationStatus)"
        }

        return "±\(Int(userLocation.horizontalAccuracy))m accuracy"
    }
}

#Preview {
    NavigationView()
}
