import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import SwiftUI

public protocol NavigationViewConfigurable where Self: View {
    var progressView: ((NavigationState?, (() -> Void)?) -> AnyView)? { get set }
    var instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)? { get set }
    var currentRoadNameView: ((NavigationState?) -> AnyView)? { get set }

    func navigationViewInstructionView(
        @ViewBuilder _ instructionsView: @escaping (NavigationState?, Binding<Bool>, Binding<CGSize>) -> some View
    ) -> Self

    func navigationViewProgressView(
        @ViewBuilder _ progressView: @escaping (NavigationState?, (() -> Void)?) -> some View
    ) -> Self

    func navigationViewCurrentRoadView(
        @ViewBuilder _ currentRoadNameView: @escaping (NavigationState?) -> some View
    ) -> Self

    @available(*, deprecated, renamed: "navigationViewCurrentRoadView")
    func navigationCurrentRoadView(
        @ViewBuilder currentRoadNameViewBuilder: @escaping () -> some View
    ) -> Self
}

public extension NavigationViewConfigurable {
    /// Override the Instructions View with a custom view.
    ///
    /// - Parameter instructionsView: The custom instructions view to display.
    /// - Returns: The modified view.
    func navigationViewInstructionView(
        @ViewBuilder _ instructionsView: @escaping (NavigationState?, Binding<Bool>, Binding<CGSize>) -> some View
    ) -> Self {
        var mutableSelf = self
        mutableSelf.instructionsView = { AnyView(instructionsView($0, $1, $2)) }
        return mutableSelf
    }

    /// Override the Progress View with a custom view.
    ///
    /// - Parameter progressView: The custom progress view to display.
    /// - Returns: The modified view.
    func navigationViewProgressView(
        @ViewBuilder _ progressView: @escaping (NavigationState?, (() -> Void)?) -> some View
    ) -> Self {
        var mutableSelf = self
        mutableSelf.progressView = { AnyView(progressView($0, $1)) }
        return mutableSelf
    }

    /// Override the Current Road Name View with a custom view.
    ///
    /// - Parameter currentRoadNameView: The custom current road name view to display.
    /// - Returns: The modified view.
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

    // MARK: Defaults

    @ViewBuilder static func defaultProgressView(_ navigationState: NavigationState?,
                                                 _ onTapExit: (() -> Void)?) -> some View
    {
        if case .navigating = navigationState?.tripState,
           let progress = navigationState?.currentProgress
        {
            TripProgressView(
                progress: progress,
                onTapExit: onTapExit
            )
        }
    }

    @ViewBuilder static func defaultInstructionsView(
        _ navigationState: NavigationState?
    ) -> some View {
        @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

        if case .navigating = navigationState?.tripState,
           let visualInstruction = navigationState?.currentVisualInstruction,
           let progress = navigationState?.currentProgress,
           let remainingSteps = navigationState?.remainingSteps
        {
            InstructionsView(
                visualInstruction: visualInstruction,
                distanceFormatter: formatterCollection.distanceFormatter,
                distanceToNextManeuver: progress.distanceToNextManeuver,
                remainingSteps: remainingSteps,
                isExpanded: .constant(false) // TODO: $isInstructionViewExpanded
            )
        }
    }

    @ViewBuilder static func defaultCurrentRoadNameView(_ navigationState: NavigationState?) -> some View {
        CurrentRoadNameView(currentRoadName: navigationState?.currentRoadName)
    }
}
