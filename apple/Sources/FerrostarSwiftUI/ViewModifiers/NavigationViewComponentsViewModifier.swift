import FerrostarCore
import SwiftUI

// MARK: - Navigation View Components Configuration Environment

public struct NavigationViewComponentsConfiguration {
    public let progressView: ((NavigationState?, (() -> Void)?) -> AnyView)?
    public let offRouteView: ((NavigationState?, Binding<CGSize>) -> AnyView)?
    public let instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)?
    public let currentRoadNameView: ((NavigationState?) -> AnyView)?

    public init(
        progressView: ((NavigationState?, (() -> Void)?) -> AnyView)? = nil,
        offRouteView: ((NavigationState?, Binding<CGSize>) -> AnyView)? = nil,
        instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)? = nil,
        currentRoadNameView: ((NavigationState?) -> AnyView)? = nil
    ) {
        self.progressView = progressView
        self.offRouteView = offRouteView
        self.instructionsView = instructionsView
        self.currentRoadNameView = currentRoadNameView
    }

    // MARK: - Convenience Accessors

    /// Get the progress view, falling back to default if none is configured.
    @ViewBuilder
    public func getProgressView(_ navigationState: NavigationState?, onTapExit: (() -> Void)?) -> some View {
        if let customProgressView = progressView {
            customProgressView(navigationState, onTapExit)
        } else {
            DefaultNavigationViewComponents.defaultProgressView(navigationState, onTapExit)
        }
    }

    @ViewBuilder
    public func getOffRouteView(
        _ navigationState: NavigationState?,
        size: Binding<CGSize>
    ) -> some View {
        if let customOffRouteView = offRouteView {
            customOffRouteView(navigationState, size)
        } else {
            DefaultNavigationViewComponents.defaultOffRouteView(navigationState, size: size)
        }
    }

    /// Get the instructions view, falling back to default if none is configured.
    @ViewBuilder
    public func getInstructionsView(
        _ navigationState: NavigationState?,
        isExpanded: Binding<Bool>,
        sizeWhenNotExpanded: Binding<CGSize>
    ) -> some View {
        if let customInstructionsView = instructionsView {
            customInstructionsView(navigationState, isExpanded, sizeWhenNotExpanded)
        } else {
            DefaultNavigationViewComponents.defaultInstructionsView(navigationState, isExpanded, sizeWhenNotExpanded)
        }
    }

    /// Get the current road name view, falling back to default if none is configured.
    @ViewBuilder
    public func getCurrentRoadNameView(_ navigationState: NavigationState?) -> some View {
        if let customRoadNameView = currentRoadNameView {
            customRoadNameView(navigationState)
        } else {
            DefaultNavigationViewComponents.defaultCurrentRoadNameView(navigationState)
        }
    }
}

private struct NavigationViewComponentsConfigurationKey: EnvironmentKey {
    static var defaultValue: NavigationViewComponentsConfiguration = .init()
}

public extension EnvironmentValues {
    var navigationViewComponentsConfiguration: NavigationViewComponentsConfiguration {
        get { self[NavigationViewComponentsConfigurationKey.self] }
        set { self[NavigationViewComponentsConfigurationKey.self] = newValue }
    }
}

// MARK: - Navigation View Components Modifier

private struct NavigationViewComponentsViewModifier: ViewModifier {
    let progressView: ((NavigationState?, (() -> Void)?) -> AnyView)?
    let offRouteView: ((NavigationState?, Binding<CGSize>) -> AnyView)?
    let instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)?
    let currentRoadNameView: ((NavigationState?) -> AnyView)?

    func body(content: Content) -> some View {
        content
            .transformEnvironment(\.navigationViewComponentsConfiguration) { config in
                // Merge new configuration with existing, prioritizing new values
                config = NavigationViewComponentsConfiguration(
                    progressView: progressView ?? config.progressView,
                    offRouteView: offRouteView ?? config.offRouteView,
                    instructionsView: instructionsView ?? config.instructionsView,
                    currentRoadNameView: currentRoadNameView ?? config.currentRoadNameView
                )
            }
    }
}

// MARK: - Type-Safe Extensions

public extension View {
    /// Configure the navigation view components that are displayed during navigation.
    ///
    /// This modifier sets navigation component configuration in the environment that can be read
    /// by child views that conform to NavigationViewComponentsHost.
    ///
    /// - Parameters:
    ///   - progressView: Custom progress view factory. If nil, uses default TripProgressView.
    ///   - instructionsView: Custom instructions view factory. If nil, uses default InstructionsView.
    ///   - currentRoadNameView: Custom road name view factory. If nil, uses default CurrentRoadNameView.
    /// - Returns: A modified view with navigation components configuration in the environment.
    func navigationViewComponents(
        progressView: ((NavigationState?, (() -> Void)?) -> AnyView)? = nil,
        offRouteView: ((NavigationState?, Binding<CGSize>) -> AnyView)? = nil,
        instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)? = nil,
        currentRoadNameView: ((NavigationState?) -> AnyView)? = nil
    ) -> some View {
        modifier(NavigationViewComponentsViewModifier(
            progressView: progressView,
            offRouteView: offRouteView,
            instructionsView: instructionsView,
            currentRoadNameView: currentRoadNameView
        ))
    }

    /// Configure the navigation view instructions component.
    ///
    /// - Parameter instructionsView: The custom instructions view to display.
    /// - Returns: A modified view with navigation components configuration in the environment.
    func navigationViewInstructionView(
        @ViewBuilder _ instructionsView: @escaping (NavigationState?, Binding<Bool>, Binding<CGSize>) -> some View
    ) -> some View {
        navigationViewComponents(instructionsView: { AnyView(instructionsView($0, $1, $2)) })
    }

    /// Configure the navigation view off route banner.
    ///
    /// - Parameter offRouteView: The custom off route banner.
    /// - Returns: A modified view with navigation components configuration in the environment.
    func navigationViewOffRouteView(
        @ViewBuilder _ offRouteView: @escaping (NavigationState?, Binding<CGSize>) -> some View
    ) -> some View {
        navigationViewComponents(offRouteView: { AnyView(offRouteView($0, $1)) })
    }

    /// Configure the navigation view progress component.
    ///
    /// - Parameter progressView: The custom progress view to display.
    /// - Returns: A modified view with navigation components configuration in the environment.
    func navigationViewProgressView(
        @ViewBuilder _ progressView: @escaping (NavigationState?, (() -> Void)?) -> some View
    ) -> some View {
        navigationViewComponents(progressView: { AnyView(progressView($0, $1)) })
    }

    /// Configure the navigation view current road name component.
    ///
    /// - Parameter currentRoadNameView: The custom current road name view to display.
    /// - Returns: A modified view with navigation components configuration in the environment.
    func navigationViewCurrentRoadView(
        @ViewBuilder _ currentRoadNameView: @escaping (NavigationState?) -> some View
    ) -> some View {
        navigationViewComponents(currentRoadNameView: { AnyView(currentRoadNameView($0)) })
    }

    @available(*, deprecated, renamed: "navigationViewCurrentRoadView")
    func navigationCurrentRoadView(
        @ViewBuilder currentRoadNameViewBuilder: @escaping () -> some View
    ) -> some View {
        navigationViewCurrentRoadView { _ in
            currentRoadNameViewBuilder()
        }
    }
}

// MARK: - Default Navigation Components

/// Static helper providing default navigation view components.
/// Views should read custom components from environment using @Environment(\.navigationViewComponentsConfiguration)
public enum DefaultNavigationViewComponents {
    // MARK: Default Views (can be overridden via environment)

    @ViewBuilder public static func defaultProgressView(
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

    @ViewBuilder public static func defaultOffRouteView(
        _: NavigationState?,
        size: Binding<CGSize>
    ) -> some View {
        OffRouteBannerView(size: size)
    }

    @ViewBuilder public static func defaultInstructionsView(
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

    @ViewBuilder public static func defaultCurrentRoadNameView(_ navigationState: NavigationState?)
        -> some View
    {
        // Only show the road view when on route.
        if case .noDeviation = navigationState?.currentDeviation {
            CurrentRoadNameView(currentRoadName: navigationState?.currentRoadName)
        }
    }
}
