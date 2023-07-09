import CoreLocation

public protocol LocationProviding: AnyObject {
    var delegate: LocationManagingDelegate? { get set }
    var location: CLLocation? { get }
    var heading: CLHeading? { get }

    func startUpdatingLocation()
    func stopUpdatingLocation()

    func startUpdatingHeading()
    func stopUpdatingHeading()
}

/// All methods are analogues for equivalents on `CLLocationManagerDelegate`
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
    public var location: CLLocation? {
        locationManager.location
    }

    public var heading: CLHeading? {
        locationManager.heading
    }

    public func startUpdatingLocation() {
        locationManager.startUpdatingLocation()
    }

    public func stopUpdatingLocation() {
        locationManager.stopUpdatingLocation()
    }

    public func startUpdatingHeading() {
        locationManager.startUpdatingHeading()
    }

    public func stopUpdatingHeading() {
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

/// Location service for testing witohut relying on simulator location spoofing.
///
/// This allows for more granular unit tests.
public class SimulatedLocationManager: LocationProviding {
    public var delegate: LocationManagingDelegate?
    public var location: CLLocation? {
        didSet {
            notifyDelegateOfLocation()
        }
    }

    public var heading: CLHeading? {
        didSet {
            notifyDelegateOfHeading()
        }
    }

    private var isUpdatingLocation = false {
        didSet {
            notifyDelegateOfLocation()
        }
    }

    private var isUpdatingHeading = false {
        didSet {
            notifyDelegateOfHeading()
        }
    }

    public func startUpdatingLocation() {
        isUpdatingLocation = true
    }

    public func stopUpdatingLocation() {
        isUpdatingLocation = false
    }

    public func startUpdatingHeading() {
        isUpdatingHeading = true
    }

    public func stopUpdatingHeading() {
        isUpdatingHeading = false
    }

    private func notifyDelegateOfLocation() {
        if isUpdatingLocation, let location = location {
            delegate?.locationManager(self, didUpdateLocations: [location])
        }
    }

    private func notifyDelegateOfHeading() {
        if isUpdatingHeading, let heading = heading {
            delegate?.locationManager(self, didUpdateHeading: heading)
        }
    }
}
