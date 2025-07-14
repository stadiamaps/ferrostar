import CoreLocation
import FerrostarCore
import FerrostarCoreFFI
import Foundation

@Observable final class SwitchableLocationProvider: LocationProviding {
    enum State {
        case simulated
        case device
    }

    private let simulated: SimulatedLocationProvider
    private let device = CoreLocationProvider(
        activityType: .automotiveNavigation,
        allowBackgroundLocationUpdates: false
    )
    var type: State

    init(simulated: SimulatedLocationProvider, type: SwitchableLocationProvider.State) {
        self.simulated = simulated
        self.type = type
    }

    private var current: LocationProviding {
        switch type {
        case .simulated:
            simulated
        case .device:
            device
        }
    }

    var delegate: (any LocationManagingDelegate)? { get {
        current.delegate
    } set {
        current.delegate = newValue
    }
    }

    var authorizationStatus: CLAuthorizationStatus {
        current.authorizationStatus
    }

    var lastLocation: FerrostarCoreFFI.UserLocation? {
        current.lastLocation
    }

    var lastHeading: FerrostarCoreFFI.Heading? {
        current.lastHeading
    }

    func startUpdating() {
        current.startUpdating()
    }

    func stopUpdating() {
        current.stopUpdating()
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
