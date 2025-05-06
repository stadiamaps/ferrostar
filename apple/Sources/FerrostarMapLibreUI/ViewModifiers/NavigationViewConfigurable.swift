import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import SwiftUI

public protocol NavigationViewConfigurable where Self: View {
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

    // MARK: Navigation Views

    var progressView: ((NavigationState?, (() -> Void)?) -> AnyView)? { get set }
    var instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)? { get set }
    var currentRoadNameView: ((NavigationState?) -> AnyView)? { get set }

    /// Override the Instructions View with a custom view.
    ///
    /// - Parameter instructionsView: The custom instructions view to display.
    /// - Returns: The modified view.
    func navigationViewInstructionView(
        @ViewBuilder _ instructionsView: @escaping (NavigationState?, Binding<Bool>, Binding<CGSize>) -> some View
    ) -> Self

    /// Override the Progress View with a custom view.
    ///
    /// - Parameter progressView: The custom progress view to display.
    /// - Returns: The modified view.
    func navigationViewProgressView(
        @ViewBuilder _ progressView: @escaping (NavigationState?, (() -> Void)?) -> some View
    ) -> Self

    /// Override the Current Road Name View with a custom view.
    ///
    /// - Parameter currentRoadNameView: The custom current road name view to display.
    /// - Returns: The modified view.
    func navigationViewCurrentRoadView(
        @ViewBuilder _ currentRoadNameView: @escaping (NavigationState?) -> some View
    ) -> Self

    @available(*, deprecated, renamed: "navigationViewCurrentRoadView")
    func navigationCurrentRoadView(
        @ViewBuilder currentRoadNameViewBuilder: @escaping () -> some View
    ) -> Self
}

public extension NavigationViewConfigurable {
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

    func navigationViewInstructionView(
        @ViewBuilder _ instructionsView: @escaping (NavigationState?, Binding<Bool>, Binding<CGSize>) -> some View
    ) -> Self {
        var mutableSelf = self
        mutableSelf.instructionsView = { AnyView(instructionsView($0, $1, $2)) }
        return mutableSelf
    }

    func navigationViewProgressView(
        @ViewBuilder _ progressView: @escaping (NavigationState?, (() -> Void)?) -> some View
    ) -> Self {
        var mutableSelf = self
        mutableSelf.progressView = { AnyView(progressView($0, $1)) }
        return mutableSelf
    }

    func navigationViewCurrentRoadView(
        @ViewBuilder _ currentRoadNameView: @escaping (NavigationState?) -> some View
    ) -> Self {
        var mutableSelf = self
        mutableSelf.currentRoadNameView = { AnyView(currentRoadNameView($0)) }
        return mutableSelf
    }

    @available(*, deprecated, renamed: "navigationViewCurrentRoadView")
    func navigationCurrentRoadView(@ViewBuilder currentRoadNameViewBuilder: @escaping () -> some View) -> Self {
        navigationViewCurrentRoadView { _ in
            currentRoadNameViewBuilder()
        }
    }
}
