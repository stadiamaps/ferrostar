import CoreLocation
import MapLibre
import MapLibreSwiftUI

/// A map location manager that can be directly fed by navigation state updates.
public protocol NavigationDrivenLocationManager: MLNLocationManager, AnyObject {
    var lastLocation: CLLocation { get set }
    var lastHeading: CLHeading? { get set }
}

extension StaticLocationManager: NavigationDrivenLocationManager {}
