import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public protocol NavigationMapViewConfigurable where Self: View {
    // MARK: MapView Config

    var mapInsets: NavigationMapViewContentInsetBundle { get set }

    /// Customize both the landscape NavigationMapView content insets.
    ///
    /// - Parameters:
    ///   - landscape: Generate the content inset for landscape mode with a given geometry proxy.
    /// - Returns: The modified view.
    func navigationViewMapContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> Self

    /// Customize both the portrait NavigationMapView content insets.
    ///
    /// - Parameters:
    ///   - portrait: Generate the content inset for portrait mode with a given geometry proxy.
    /// - Returns: The modified view.
    func navigationViewMapContentInset(
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> Self

    /// Customize both the landscape and portrait NavigationMapView content insets.
    ///
    /// - Parameters:
    ///   - landscape: Generate the content inset for landscape mode with a given geometry proxy.
    ///   - portrait: Generate the content inset for portrait mode with a given geometry proxy.
    /// - Returns: The modified view.
    func navigationViewMapContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode,
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> Self

    // MARK: Route Override

    var routeLayerOverride: ((NavigationState?) -> any StyleLayerCollection)? { get set }

    /// Override the Route's Style Layer.
    ///
    /// Important! This can be used to add any StyleLayerCollection, but it will replace the default route line.
    /// You should use the NavigationMapView's `_ makeMapContent` input for general purpose content.
    ///
    /// - Parameter content: The new route style layer.
    /// - Returns: The modified view
    func navigationMapViewRoute(
        @MapViewContentBuilder content: @escaping (NavigationState?) -> some StyleLayerCollection
    ) -> Self
}

public extension NavigationMapViewConfigurable {
    func navigationViewMapContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> Self {
        var mutableSelf = self
        mutableSelf.mapInsets = NavigationMapViewContentInsetBundle(landscape: landscape)
        return mutableSelf
    }

    func navigationViewMapContentInset(
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> Self {
        var mutableSelf = self
        mutableSelf.mapInsets = NavigationMapViewContentInsetBundle(portrait: portrait)
        return mutableSelf
    }

    func navigationViewMapContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode,
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> Self {
        var mutableSelf = self
        mutableSelf.mapInsets = NavigationMapViewContentInsetBundle(landscape: landscape, portrait: portrait)
        return mutableSelf
    }

    func navigationMapViewRoute(
        @MapViewContentBuilder content: @escaping (NavigationState?) -> some StyleLayerCollection
    ) -> Self {
        var mutableSelf = self
        mutableSelf.routeLayerOverride = content
        return mutableSelf
    }
}
