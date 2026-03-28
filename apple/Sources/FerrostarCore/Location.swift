import CoreLocation
import FerrostarCoreFFI

@MainActor
public protocol LocationProviding: AnyObject {
    var delegate: (any LocationManagingDelegate)? { get set }
    var authorizationStatus: CLAuthorizationStatus { get }
    var lastLocation: UserLocation? { get }
    var lastHeading: Heading? { get }

    func startUpdating()
    func stopUpdating()
}

@MainActor
public protocol LocationManagingDelegate: AnyObject {
    func locationManager(_ manager: any LocationProviding, didUpdateLocations locations: [UserLocation])
    func locationManager(_ manager: any LocationProviding, didUpdateHeading newHeading: Heading)
    func locationManager(_ manager: any LocationProviding, didFailWithError error: Error)
}

/// A location provider that uses Apple's CoreLocation framework.
@MainActor
public class CoreLocationProvider: NSObject {
    public var delegate: (any LocationManagingDelegate)?
    public private(set) var authorizationStatus: CLAuthorizationStatus

    private let locationManager: CLLocationManager

    /// The activity type of the inner CLLocationManager
    public var activityType: CLActivityType {
        get { locationManager.activityType }
        set { locationManager.activityType = newValue }
    }

    /// Creates a location provider backed by an internal `CLLocationManager`
    /// using the stated activity type.
    ///
    /// If authorization is not determined, this will automatically request permission to use location
    /// while the user is using the app.
    /// If for some reason your app requires access to the user's location at all times
    /// (rarely necessary except for significant change monitoring and geofencing notifications),
    /// you will need to request this access yourself.
    /// Refer to the CoreLocation framework guides for further information.
    public init(activityType: CLActivityType, allowBackgroundLocationUpdates: Bool) {
        locationManager = CLLocationManager()
        authorizationStatus = locationManager.authorizationStatus

        super.init()

        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        locationManager.allowsBackgroundLocationUpdates = allowBackgroundLocationUpdates

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

extension CoreLocationProvider: @preconcurrency CLLocationManagerDelegate {
    nonisolated public func locationManager(_: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let userLocations = locations.map(\.userLocation)
        let lastUserLocation = locations.last?.userLocation
        Task { @MainActor [weak self] in
            guard let self else { return }
            self.lastLocation = lastUserLocation
            self.delegate?.locationManager(self, didUpdateLocations: userLocations)
        }
    }

    nonisolated public func locationManager(_: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        let value = Heading(clHeading: newHeading)
        Task { @MainActor [weak self] in
            guard let self else { return }
            self.lastHeading = value
            if let value {
                self.delegate?.locationManager(self, didUpdateHeading: value)
            }
        }
    }

    nonisolated public func locationManager(_: CLLocationManager, didFailWithError error: Error) {
        let sendableError = error as NSError // NSError is Sendable
        Task { @MainActor [weak self] in
            guard let self else { return }
            self.delegate?.locationManager(self, didFailWithError: sendableError)
        }
    }

    nonisolated public func locationManagerDidChangeAuthorization(_: CLLocationManager) {
        Task { @MainActor [weak self] in
            guard let self else { return }
            self.authorizationStatus = self.locationManager.authorizationStatus

            switch self.authorizationStatus {
            case .authorizedAlways, .authorizedWhenInUse:
                self.locationManager.requestLocation()
            default: break
            }
        }
    }
}

/// Location provider for testing without relying on simulator location spoofing.
///
/// This allows for more granular unit tests as well as route simulation use cases.
@MainActor
public class SimulatedLocationProvider: LocationProviding {
    public var delegate: (any LocationManagingDelegate)?
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

    public func setSimulatedRoute(
        _ route: Route,
        resampleDistance: Double = 10,
        bias: LocationBias = .none
    ) throws {
        simulationState = try locationSimulationFromRoute(
            route: route,
            resampleDistance: resampleDistance,
            bias: bias
        )
        lastLocation = simulationState?.currentLocation
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
        var pendingCompletion = false

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
                if pendingCompletion {
                    stopUpdating()
                    return
                } else {
                    pendingCompletion = true
                }
            }

            // Bump the last location.
            lastLocation = updatedState.currentLocation
            simulationState = updatedState
        }
    }
}
