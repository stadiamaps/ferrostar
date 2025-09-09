import FerrostarCore
import FerrostarSwiftUI
import MapLibreSwiftDSL
import SwiftUI

// MARK: - Navigation Route Overlay Config

public struct NavigationMapViewRouteOverlayConfiguration {
    let routeOverlay: (NavigationState?) -> any StyleLayerCollection

    @MapViewContentBuilder static func `default`(_ navigationState: NavigationState?) -> some StyleLayerCollection {
        if let routePolyline = navigationState?.routePolyline {
            RouteStyleLayer(polyline: routePolyline,
                            identifier: "route-polyline",
                            style: TravelledRouteStyle())
        }

        if let remainingRoutePolyline = navigationState?.remainingRoutePolyline {
            RouteStyleLayer(polyline: remainingRoutePolyline,
                            identifier: "remaining-route-polyline")
        }
    }
}

private struct NavigationMapViewRouteOverlayConfigurationKey: EnvironmentKey {
    static var defaultValue: NavigationMapViewRouteOverlayConfiguration = .init { navigationState in
        NavigationMapViewRouteOverlayConfiguration.default(navigationState)
    }
}

public extension EnvironmentValues {
    var navigationMapViewRouteOverlayConfiguration: NavigationMapViewRouteOverlayConfiguration {
        get { self[NavigationMapViewRouteOverlayConfigurationKey.self] }
        set { self[NavigationMapViewRouteOverlayConfigurationKey.self] = newValue }
    }
}

// MARK: - Navigation Map Content Insets Modifier

private struct NavigationMapViewRouteOverlayViewModifier: ViewModifier {
    let routeOverlay: ((NavigationState?) -> any StyleLayerCollection)?

    func body(content: Content) -> some View {
        content
            .transformEnvironment(\.navigationMapViewRouteOverlayConfiguration) { config in
                // Merge new configuration with existing, prioritizing new values
                config = NavigationMapViewRouteOverlayConfiguration(
                    routeOverlay: routeOverlay ?? config.routeOverlay
                )
            }
    }
}

// MARK: - View Extensions

public extension View {
    /// Override the map view's route polyline overlay.
    ///
    /// - Parameter routeOverlay: The MapView content builder that produces the NavigationMapView's route overlay.
    /// - Returns: A modified view heirarchy.
    func navigationMapViewRouteOverlay(
        @MapViewContentBuilder routeOverlay: @escaping ((NavigationState?) -> some StyleLayerCollection)
    ) -> some View {
        modifier(NavigationMapViewRouteOverlayViewModifier(
            routeOverlay: routeOverlay
        ))
    }
}
