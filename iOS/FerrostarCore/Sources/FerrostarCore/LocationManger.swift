import CoreLocation

public protocol LocationManager: AnyObject {
    // TODO: new delegate protocol?
    var delegate: CLLocationManagerDelegate? { get set }
    var location: CLLocation? { get }
    
    func startUpdatingLocation()
    func stopUpdatingLocation()

    func startUpdatingHeading()
    func stopUpdatingHeading()
}

public class LiveLocationManager {
    private let locationManager = CLLocationManager()

    public init(activityType: CLActivityType) {
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation

        switch (locationManager.authorizationStatus) {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        default:
            break  // No action
        }

        locationManager.activityType = activityType
    }
}

extension LiveLocationManager: LocationManager {
    public var delegate: CLLocationManagerDelegate? {
        get {
            locationManager.delegate
        }
        set {
            locationManager.delegate = newValue
        }
    }

    public var location: CLLocation? {
        locationManager.location
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

// TODO: Simulated location manager
