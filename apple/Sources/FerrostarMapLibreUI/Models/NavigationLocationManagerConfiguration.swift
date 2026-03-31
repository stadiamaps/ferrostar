import MapLibre
import MapLibreSwiftUI

/// Configures location managers used by Ferrostar map navigation views.
///
/// - `nonNavigatingLocationManager`:
///   - `nil` uses MapLibre's default internal manager (recommended default).
///   - any custom manager can be supplied for non-navigation behavior.
/// - `navigatingLocationManager`:
///   - custom manager fed by Ferrostar navigation state.
///
/// Note: `MLNLocationManager` implementations are reference types. Do not construct this configuration
/// inline in a SwiftUI `body`; keep manager instances in stable state/model storage and pass references here.
public struct NavigationLocationManagerConfiguration {
    public var nonNavigatingLocationManager: (any MLNLocationManager)?
    public var navigatingLocationManager: any NavigationDrivenLocationManager

    public init(
        nonNavigatingLocationManager: (any MLNLocationManager)? = nil,
        navigatingLocationManager: any NavigationDrivenLocationManager
    ) {
        self.nonNavigatingLocationManager = nonNavigatingLocationManager
        self.navigatingLocationManager = navigatingLocationManager
    }
}
