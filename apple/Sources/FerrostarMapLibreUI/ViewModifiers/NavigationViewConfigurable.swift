import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import SwiftUI

public protocol NavigationViewConfigurable where Self: View {
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
