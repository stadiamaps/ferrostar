import CoreLocation
import FerrostarCoreFFI

public protocol LocationProviding: AnyObject {
    var delegate: LocationManagingDelegate? { get set }
    var authorizationStatus: CLAuthorizationStatus { get }
    var lastLocation: UserLocation? { get }
    var lastHeading: Heading? { get }

    func startUpdating()
    func stopUpdating()
}

public protocol LocationManagingDelegate: AnyObject {
    func locationManager(_ manager: LocationProviding, didUpdateLocations locations: [UserLocation])
    func locationManager(_ manager: LocationProviding, didUpdateHeading newHeading: Heading)
    func locationManager(_ manager: LocationProviding, didFailWithError error: Error)
}

/// A location provider that uses Apple's CoreLocation framework.
public class CoreLocationProvider: NSObject, ObservableObject {
    public var delegate: LocationManagingDelegate?
    public private(set) var authorizationStatus: CLAuthorizationStatus

    private let locationManager: CLLocationManager

    /// Creates a location provider backed by an internal `CLLocationManager`
    /// using the stated activity type.
    ///
    /// If authorization is not determined, this will automatically request permission to use location
    /// while the user is using the app.
    /// If for some reason your app requires access to the user's location at all times
    /// (rarely necessary except for significant change monitoring and geofencing notifications),
    /// you will need to request this access yourself.
    /// Refer to the CoreLocation framework guides for further information.
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
            lastLocation = locationManager.location?.userLocation
            locationManager.requestLocation()
        default:
            break
        }

        locationManager.activityType = activityType
    }

    @Published public private(set) var lastLocation: UserLocation?

    @Published public private(set) var lastHeading: Heading?
}

extension CoreLocationProvider: LocationProviding {
    public func startUpdating() {
        locationManager.startUpdatingLocation()
        locationManager.startUpdatingHeading()
    }

    public func stopUpdating() {
        locationManager.stopUpdatingLocation()
        locationManager.stopUpdatingHeading()
    }
}

extension CoreLocationProvider: CLLocationManagerDelegate {
    public func locationManager(_: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        lastLocation = locations.last?.userLocation
        delegate?.locationManager(self, didUpdateLocations: locations.map(\.userLocation))
    }

    public func locationManager(_: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        let value = Heading(clHeading: newHeading)
        lastHeading = value

        if let value {
            delegate?.locationManager(self, didUpdateHeading: value)
        }
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
/// This allows for more granular unit tests as well as route simulation use cases.
public class SimulatedLocationProvider: LocationProviding, ObservableObject {
    public var delegate: LocationManagingDelegate?
    public private(set) var authorizationStatus: CLAuthorizationStatus = .authorizedAlways

    private var updateTask: Task<Void, Error>?

    private var simulationState: LocationSimulationState?

    /// A factor by which simulated route playback speed is multiplied.
    public var warpFactor: UInt64 = 1

    @Published public var lastLocation: UserLocation? {
        didSet {
            notifyDelegateOfLocation()
        }
    }

    @Published public var lastHeading: Heading? {
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
        lastLocation = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude).userLocation
    }

    public init(location: CLLocation) {
        lastLocation = location.userLocation
    }

    public init(location: UserLocation) {
        lastLocation = location
    }

    public func setSimulatedRoute(_ route: Route, resampleDistance: Double = 10) throws {
        simulationState = try locationSimulationFromRoute(route: route, resampleDistance: resampleDistance)
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

            guard let initialState = simulationState else {
                return
            }

            try await Task.sleep(nanoseconds: NSEC_PER_SEC / warpFactor)

            // Check cancellation before updating after wait.
            try Task.checkCancellation()

            // Calculate the new state.
            let updatedState = advanceLocationSimulation(state: initialState)

            // Stop if the route has been fully simulated (no state change).
            if initialState == updatedState {
                stopUpdating()
                return
            }

            // Bump the last location.
            lastLocation = updatedState.currentLocation
            simulationState = updatedState
        }
    }
}
