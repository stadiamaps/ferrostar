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
public class LiveLocationManager: NSObject {
    public var delegate: LocationManagingDelegate?

    private let locationManager: CLLocationManager

    public init(activityType: CLActivityType) {
        locationManager = CLLocationManager()
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation

        switch locationManager.authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        default:
            break // No action
        }

        locationManager.activityType = activityType
        super.init()

        locationManager.delegate = self
    }
}

extension LiveLocationManager: LocationProviding {
    public var lastLocation: CLLocation? {
        locationManager.location
    }

    public var lastHeading: CLHeading? {
        locationManager.heading
    }

    public func startUpdating() {
        locationManager.startUpdatingLocation()
        locationManager.startUpdatingHeading()
    }

    public func stopUpdating() {
        locationManager.stopUpdatingLocation()
        locationManager.stopUpdatingHeading()
    }
}

extension LiveLocationManager: CLLocationManagerDelegate {
    public func locationManager(_: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        delegate?.locationManager(self, didUpdateLocations: locations)
    }

    public func locationManager(_: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        delegate?.locationManager(self, didUpdateHeading: newHeading)
    }

    public func locationManager(_: CLLocationManager, didFailWithError error: Error) {
        delegate?.locationManager(self, didFailWithError: error)
    }
}

/// Location provider for testing without relying on simulator location spoofing.
///
/// This allows for more granular unit tests.
public class SimulatedLocationManager: LocationProviding {
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
