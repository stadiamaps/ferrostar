import CoreLocation
import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

let style = URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(APIKeys.shared.stadiaMapsAPIKey)")!
private let initialLocation = CLLocation(latitude: 37.332726,
                                         longitude: -122.031790)

struct DemoNavigationView: View {
    private let navigationDelegate = NavigationDelegate()
    // NOTE: This is probably not ideal but works for demo purposes.
    // This causes a thread performance checker warning log.
    private let spokenInstructionObserver = SpokenInstructionObserver.initAVSpeechSynthesizer(isMuted: false)

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
    @State private var snappedCamera = true

    init() {
        let simulated = SimulatedLocationProvider(location: initialLocation)
        simulated.warpFactor = 2
        locationProvider = simulated

        // Configure the navigation session.
        // You have a lot of flexibility here based on your use case.
        let config = SwiftNavigationControllerConfig(
            stepAdvance: .relativeLineStringDistance(minimumHorizontalAccuracy: 32, automaticAdvanceDistance: 10),
            routeDeviationTracking: .staticThreshold(minimumHorizontalAccuracy: 25, maxAcceptableDeviation: 20),
            snappedLocationCourseFiltering: .snapToRoute
        )

        ferrostarCore = try! FerrostarCore(
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
        // NOTE: Not all applications will need a delegate. Read the NavigationDelegate documentation for details.
        ferrostarCore.delegate = navigationDelegate

        // Initialize text-to-speech; note that this is NOT automatic.
        // You must set a spokenInstructionObserver.
        // Fortunately, this is pretty easy with the provided class
        // backed by AVSpeechSynthesizer.
        // You can customize the instance it further as needed,
        // or replace with your own.
        ferrostarCore.spokenInstructionObserver = spokenInstructionObserver
    }

    var body: some View {
        let locationServicesEnabled = locationProvider.authorizationStatus == .authorizedAlways
            || locationProvider.authorizationStatus == .authorizedWhenInUse

        NavigationStack {
            DynamicallyOrientingNavigationView(
                styleURL: style,
                camera: $camera,
                navigationState: ferrostarCore.state,
                isMuted: spokenInstructionObserver.isMuted,
                onTapMute: spokenInstructionObserver.toggleMute,
                onTapExit: { stopNavigation() },
                makeMapContent: {
                    let source = ShapeSource(identifier: "userLocation") {
                        // Demonstrate how to add a dynamic overlay;
                        // also incidentally shows the extent of puck lag
                        if let userLocation = locationProvider.lastLocation {
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
                                            try await startNavigation()
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
                                .shadow(radius: 10)
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

        if let simulated = locationProvider as? SimulatedLocationProvider {
            // This configures the simulator to the desired route.
            // The ferrostarCore.startNavigation will still start the location
            // provider/simulator.
            try simulated.setSimulatedRoute(route, resampleDistance: 5)
            print("DemoApp: setting route to be simulated")
        }

        // Starts the navigation state machine.
        // It's worth having a look through the parameters,
        // as most of the configuration happens here.
        try ferrostarCore.startNavigation(route: route)

        preventAutoLock()
    }

    func stopNavigation() {
        ferrostarCore.stopNavigation()
        camera = .center(initialLocation.coordinate, zoom: 14)
        allowAutoLock()
    }

    var locationLabel: String {
        guard let userLocation = locationProvider.lastLocation else {
            return "No location - authed as \(locationProvider.authorizationStatus)"
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
