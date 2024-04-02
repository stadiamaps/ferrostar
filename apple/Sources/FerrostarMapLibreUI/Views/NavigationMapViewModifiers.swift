import Foundation
import SwiftUI

public extension NavigationMapView {
    /// Set the MapView's content inset to a dynamically controlled navigation setting.
    ///
    /// This functionality is used to appropriate space the navigation puck/user location in the map view.
    ///
    /// - Parameter inset: The inset mode for the navigation map view
    /// - Returns: The modified NavigationMapView
    func navigationMapViewContentInset(_ inset: NavigationMapViewContentInsetMode) -> NavigationMapView {
        var newNavigationMapView = self
        newNavigationMapView.mapViewContentInset = inset.uiEdgeInsets
        return newNavigationMapView
    }
}
