import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import SwiftUI

/// An internal class for passing in the NavigationView components.
///
/// A downstream developer would use the navigationView modifiers to override these
/// on a parent NavigationView.
struct NavigationViewComponentBuilder {
    var progressView: (NavigationState?, (() -> Void)?) -> AnyView
    var instructionsView: (NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView
    var currentRoadNameView: (NavigationState?) -> AnyView

    init(
        progressView: ((NavigationState?, (() -> Void)?) -> AnyView)?,
        instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)?,
        currentRoadNameView: ((NavigationState?) -> AnyView)?
    ) {
        self.progressView = progressView ?? { AnyView(Self.defaultProgressView($0, $1)) }
        self.instructionsView =
            instructionsView ?? { AnyView(Self.defaultInstructionsView($0, $1, $2)) }
        self.currentRoadNameView =
            currentRoadNameView ?? { AnyView(Self.defaultCurrentRoadNameView($0)) }
    }

    // MARK: Default Views (can be overridden)

    @ViewBuilder private static func defaultProgressView(
        _ navigationState: NavigationState?, _ onTapExit: (() -> Void)?
    ) -> some View {
        if case .navigating = navigationState?.tripState,
           let progress = navigationState?.currentProgress
        {
            TripProgressView(
                progress: progress,
                onTapExit: onTapExit
            )
        }
    }

    @ViewBuilder private static func defaultInstructionsView(
        _ navigationState: NavigationState?,
        _ isExpanded: Binding<Bool>,
        _ sizeWhenNotExpanded: Binding<CGSize>
    ) -> some View {
        InstructionsViewWrapper(
            navigationState: navigationState,
            isExpanded: isExpanded,
            sizeWhenNotExpanded: sizeWhenNotExpanded
        )
    }

    @ViewBuilder private static func defaultCurrentRoadNameView(_ navigationState: NavigationState?)
        -> some View
    {
        CurrentRoadNameView(currentRoadName: navigationState?.currentRoadName)
    }
}
