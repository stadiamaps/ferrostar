import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import MapLibreSwiftDSL
import SwiftUI

public protocol NavigationViewConfigurable where Self: View {
    // MARK: Navigation Controls

    var showMute: Bool { get set }

    /// Manage whether the mute control is visible or hidden.
    ///
    /// - Parameter hidden: The view is hidden if true
    /// - Returns: The modified view.
    func navigationViewMuteControlHidden(_ hidden: Bool) -> Self

    var showZoom: Bool { get set }

    /// Manage whether the zoom control is visible or hidden.
    ///
    /// - Parameter hidden: The view is hidden if true
    /// - Returns: The modified view.
    func navigationViewZoomControlHidden(_ hidden: Bool) -> Self

    var showCentering: Bool { get set }

    /// Manage whether the centering control is visible or hidden.
    ///
    /// - Parameter hidden: The view is hidden if true
    /// - Returns: The modified view.
    func navigationViewCenteringControlHidden(_ hidden: Bool) -> Self

    // MARK: Navigation Views

    var progressView: ((NavigationState?, (() -> Void)?) -> AnyView)? { get set }
    var instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)? { get set }

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

    @available(*, deprecated, renamed: "navigationViewCurrentRoadView")
    func navigationCurrentRoadView(
        @ViewBuilder currentRoadNameViewBuilder: @escaping () -> some View
    ) -> Self
}

public extension NavigationViewConfigurable {
    func navigationViewMuteControlHidden(_ hidden: Bool) -> Self {
        var mutableSelf = self
        mutableSelf.showMute = !hidden
        return mutableSelf
    }

    func navigationViewZoomControlHidden(_ hidden: Bool) -> Self {
        var mutableSelf = self
        mutableSelf.showZoom = !hidden
        return mutableSelf
    }

    func navigationViewCenteringControlHidden(_ hidden: Bool) -> Self {
        var mutableSelf = self
        mutableSelf.showCentering = !hidden
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
}
