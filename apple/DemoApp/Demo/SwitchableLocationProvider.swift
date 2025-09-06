import Combine
import CoreLocation
import FerrostarCore
import FerrostarCoreFFI
import Foundation

final class SwitchableLocationProvider: LocationProviding {
    enum State: Equatable {
        case simulated
        case device
    }

    private let simulated: SimulatedLocationProvider
    private let device = CoreLocationProvider(
        activityType: .automotiveNavigation,
        allowBackgroundLocationUpdates: false
    )
    @Published var type: State

    init(simulated: SimulatedLocationProvider, type: SwitchableLocationProvider.State) {
        self.simulated = simulated
        self.type = type

        // Don't do this in a real app, but for the purposes of easy startup/simulation
        lastLocation = UserLocation(clLocation: AppDefaults.initialLocation)
    }

    private var current: LocationProviding {
        switch type {
        case .simulated:
            simulated
        case .device:
            device
        }
    }

    var delegate: (any LocationManagingDelegate)? {
        get { current.delegate }
        set { current.delegate = newValue }
    }

    var authorizationStatus: CLAuthorizationStatus {
        current.authorizationStatus
    }

    @Published var lastLocation: UserLocation?
    @Published var lastHeading: Heading?

    private var cancellables = Set<AnyCancellable>()

    func startUpdating() {
        current.startUpdating()

        current.lastLocation.publisher.sink {
            self.lastLocation = $0
        }.store(in: &cancellables)

        current.lastHeading.publisher.sink {
            self.lastHeading = $0
        }.store(in: &cancellables)
    }

    func stopUpdating() {
        current.stopUpdating()
        cancellables.removeAll()
    }

    func use(route: Route) throws {
        // This configures the simulator to the desired route.
        // The ferrostarCore.startNavigation will still start the location
        // provider/simulator.
        simulated
            .lastLocation = UserLocation(clCoordinateLocation2D: route.geometry.first!.clLocationCoordinate2D)
        try simulated.setSimulatedRoute(route, resampleDistance: 5)
    }

    var locationServicesEnabled: Bool {
        current.authorizationStatus == .authorizedAlways || current.authorizationStatus == .authorizedWhenInUse
    }

    func toggle() {
        current.stopUpdating()
        let delegate = current.delegate

        switch type {
        case .simulated:
            type = .device
        case .device:
            type = .simulated
        }

        current.delegate = delegate
        current.startUpdating()
    }
}
