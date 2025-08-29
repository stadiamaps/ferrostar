import FerrostarCore
import Foundation
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public extension NavigationMapView {
    /// Set the MapView's content inset. See ``NavigationMapViewContentInsetMode`` for static and dynamic options.
    ///
    /// This functionality is used to position the navigation puck/user location in the map view
    ///
    /// - Parameter inset: The inset mode for the navigation map view
    /// - Returns: The modified NavigationMapView
    func navigationMapViewContentInset(_ inset: NavigationMapViewContentInsetMode) -> NavigationMapView {
        var newNavigationMapView = self
        newNavigationMapView.mapViewContentInset = inset.uiEdgeInsets
        return newNavigationMapView
    }

    /// Override the Route's Style Layer.
    ///
    /// Important! This can be used to add any StyleLayerCollection, but it will replace the default route line.
    /// You should use the NavigationMapView's `_ makeMapContent` input for general purpose content.
    ///
    /// - Parameter content: The new route style layer.
    /// - Returns: The modified NavigationMapView
    func navigationMapViewRoute(@MapViewContentBuilder content: @escaping (NavigationState?)
        -> some StyleLayerCollection) -> NavigationMapView
    {
        var newNavigationMapView = self
        newNavigationMapView.routeLayerOverride = content
        return newNavigationMapView
    }
}
