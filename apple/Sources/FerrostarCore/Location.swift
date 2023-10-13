import CoreLocation

public protocol LocationProviding: AnyObject {
    var delegate: LocationManagingDelegate? { get set }
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
@Observable
public class LiveLocationProvider: NSObject {
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
        default:
            break // No action
        }

        locationManager.activityType = activityType
    }

    public private(set) var lastLocation: CLLocation?

    public private(set) var lastHeading: CLHeading?
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
        self.lastLocation = locations.last
        delegate?.locationManager(self, didUpdateLocations: locations)
    }

    public func locationManager(_: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        self.lastHeading = newHeading
        delegate?.locationManager(self, didUpdateHeading: newHeading)
    }

    public func locationManager(_: CLLocationManager, didFailWithError error: Error) {
        delegate?.locationManager(self, didFailWithError: error)
    }

    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = locationManager.authorizationStatus
    }
}

/// Location provider for testing without relying on simulator location spoofing.
///
/// This allows for more granular unit tests.
@Observable
public class SimulatedLocationProvider: LocationProviding {
    public var delegate: LocationManagingDelegate?
    public var lastLocation: CLLocation? {
        didSet {
            notifyDelegateOfLocation()
        }
    }

    public var lastHeading: CLHeading? {
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

    public func startUpdating() {
        isUpdating = true
    }

    public func stopUpdating() {
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
}
