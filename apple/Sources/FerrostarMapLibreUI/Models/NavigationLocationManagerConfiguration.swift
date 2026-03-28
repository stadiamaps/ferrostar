import CoreLocation
import MapLibre
import MapLibreSwiftUI

/// Configures location managers used by Ferrostar map navigation views.
///
/// - `nonNavigatingLocationManager`:
///   - `nil` uses MapLibre's default internal manager (recommended default).
///   - any custom manager can be supplied for non-navigation behavior.
/// - `navigatingLocationManager`:
///   - defaults to ``StaticLocationManager`` fed by Ferrostar navigation state.
public struct NavigationLocationManagerConfiguration {
    public var nonNavigatingLocationManager: (any MLNLocationManager)?
    public var navigatingLocationManager: any NavigationDrivenLocationManager

    public init(
        nonNavigatingLocationManager: (any MLNLocationManager)? = nil,
        navigatingLocationManager: any NavigationDrivenLocationManager =
            StaticLocationManager(initialLocation: CLLocation())
    ) {
        self.nonNavigatingLocationManager = nonNavigatingLocationManager
        self.navigatingLocationManager = navigatingLocationManager
    }

    public static var `default`: Self {
        .init()
    }
}
