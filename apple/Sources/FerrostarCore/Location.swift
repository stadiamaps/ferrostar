import CoreLocation
import UniFFI

public protocol LocationProviding: AnyObject {
    var delegate: LocationManagingDelegate? { get set }
    var authorizationStatus: CLAuthorizationStatus { get }
    var lastLocation: CLLocation? { get }
    var lastHeading: CLHeading? { get }

    func startUpdating()
    func stopUpdating()
}

public protocol LocationManagingDelegate: AnyObject {
    func locationManager(_ manager: LocationProviding, didUpdateLocations locations: [CLLocation])
    func locationManager(_ manager: LocationProviding, didUpdateHeading newHeading: CLHeading)
    func locationManager(_ manager: LocationProviding, didFailWithError error: Error)
}

// TODO: Permissions are currently NOT handled and they should be!!!
public class LiveLocationProvider: NSObject, ObservableObject {
    public var delegate: LocationManagingDelegate?
    public private(set) var authorizationStatus: CLAuthorizationStatus

    private let locationManager: CLLocationManager

    public init(activityType: CLActivityType) {
        locationManager = CLLocationManager()
        authorizationStatus = locationManager.authorizationStatus

        super.init()

        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation

        switch locationManager.authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedAlways, .authorizedWhenInUse:
            lastLocation = locationManager.location
            locationManager.requestLocation()
        default:
            break
        }

        locationManager.activityType = activityType
    }

    @Published public private(set) var lastLocation: CLLocation?

    @Published public private(set) var lastHeading: CLHeading?
}

extension LiveLocationProvider: LocationProviding {
    public func startUpdating() {
        locationManager.startUpdatingLocation()
        locationManager.startUpdatingHeading()
    }

    public func stopUpdating() {
        locationManager.stopUpdatingLocation()
        locationManager.stopUpdatingHeading()
    }
}

extension LiveLocationProvider: CLLocationManagerDelegate {
    public func locationManager(_: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        lastLocation = locations.last
        delegate?.locationManager(self, didUpdateLocations: locations)
    }

    public func locationManager(_: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        lastHeading = newHeading
        delegate?.locationManager(self, didUpdateHeading: newHeading)
    }

    public func locationManager(_: CLLocationManager, didFailWithError error: Error) {
        delegate?.locationManager(self, didFailWithError: error)
    }

    public func locationManagerDidChangeAuthorization(_: CLLocationManager) {
        authorizationStatus = locationManager.authorizationStatus

        switch authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            locationManager.requestLocation()
        default: break
        }
    }
}

/// Location provider for testing without relying on simulator location spoofing.
///
/// This allows for more granular unit tests.
public class SimulatedLocationProvider: LocationProviding, ObservableObject {
    public var delegate: LocationManagingDelegate?
    public private(set) var authorizationStatus: CLAuthorizationStatus = .authorizedAlways

    private var updateTask: Task<Void, Error>?

    public private(set) var simulationState: LocationSimulationState?
    public var warpFactor: UInt64 = 1

    @Published public var lastLocation: CLLocation? {
        didSet {
            notifyDelegateOfLocation()
        }
    }

    @Published public var lastHeading: CLHeading? {
        didSet {
            notifyDelegateOfHeading()
        }
    }

    private var isUpdating = false {
        didSet {
            notifyDelegateOfLocation()
            notifyDelegateOfHeading()
        }
    }

    public init() {}

    public init(coordinate: CLLocationCoordinate2D) {
        lastLocation = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
    }

    public init(location: CLLocation) {
        lastLocation = location
    }

    public func setSimulatedRoute(_ route: Route) throws {
        simulationState = try locationSimulationFromRoute(route: route.inner)
    }

    public func startUpdating() {
        isUpdating = true
        updateTask?.cancel()
        updateTask = Task {
            try await updateLocation()
        }
    }

    public func stopUpdating() {
        updateTask?.cancel()
        isUpdating = false
    }

    private func notifyDelegateOfLocation() {
        if isUpdating, let location = lastLocation {
            delegate?.locationManager(self, didUpdateLocations: [location])
        }
    }

    private func notifyDelegateOfHeading() {
        if isUpdating, let heading = lastHeading {
            delegate?.locationManager(self, didUpdateHeading: heading)
        }
    }

    private func updateLocation() async throws {
        while isUpdating {
            // Exit if the task has been cancelled.
            try Task.checkCancellation()

            guard let lastState = simulationState else {
                return
            }

            try await Task.sleep(nanoseconds: NSEC_PER_SEC / warpFactor)

            // Check cancellation before updating after wait.
            try Task.checkCancellation()

            // Calculate the new state.
            let newState = advanceLocationSimulation(state: lastState, speed: .jumpToNextLocation)

            // Exit/stop if the route has been fully simplated (newState location matches our existing location).
            if simulationState?.currentLocation == newState.currentLocation {
                stopUpdating()
                return
            }

            // Bump the last location.
            lastLocation = CLLocation(latitude: newState.currentLocation.lat, longitude: newState.currentLocation.lng)
            simulationState = newState
        }
    }
}
