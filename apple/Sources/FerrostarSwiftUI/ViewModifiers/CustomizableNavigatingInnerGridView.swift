import SwiftUI

// MARK: - Inner Grid Configuration Environment

public struct NavigationInnerGridConfiguration {
    public let topCenter: (() -> AnyView)?
    public let topTrailing: (() -> AnyView)?
    public let midLeading: (() -> AnyView)?
    public let bottomLeading: (() -> AnyView)?
    public let bottomTrailing: (() -> AnyView)?

    public init(
        topCenter: (() -> AnyView)? = nil,
        topTrailing: (() -> AnyView)? = nil,
        midLeading: (() -> AnyView)? = nil,
        bottomLeading: (() -> AnyView)? = nil,
        bottomTrailing: (() -> AnyView)? = nil
    ) {
        self.topCenter = topCenter
        self.topTrailing = topTrailing
        self.midLeading = midLeading
        self.bottomLeading = bottomLeading
        self.bottomTrailing = bottomTrailing
    }

    // MARK: - Convenience Accessors

    /// Get the top center view, falling back to default if none is configured.
    @ViewBuilder
    public func getTopCenter() -> some View {
        if let view = topCenter {
            view()
        } else {
            Spacer()
        }
    }

    /// Get the top trailing view, falling back to default if none is configured.
    @ViewBuilder
    public func getTopTrailing() -> some View {
        if let view = topTrailing {
            view()
        } else {
            Spacer()
        }
    }

    /// Get the mid leading view, falling back to default if none is configured.
    @ViewBuilder
    public func getMidLeading() -> some View {
        if let view = midLeading {
            view()
        } else {
            Spacer()
        }
    }

    /// Get the bottom leading view, falling back to default if none is configured.
    @ViewBuilder
    public func getBottomLeading() -> some View {
        if let view = bottomLeading {
            view()
        } else {
            Spacer()
        }
    }

    /// Get the bottom trailing view, falling back to default if none is configured.
    @ViewBuilder
    public func getBottomTrailing() -> some View {
        if let view = bottomTrailing {
            view()
        } else {
            Spacer()
        }
    }
}

private struct NavigationInnerGridConfigurationKey: EnvironmentKey {
    static var defaultValue: NavigationInnerGridConfiguration = .init()
}

public extension EnvironmentValues {
    var navigationInnerGridConfiguration: NavigationInnerGridConfiguration {
        get { self[NavigationInnerGridConfigurationKey.self] }
        set { self[NavigationInnerGridConfigurationKey.self] = newValue }
    }
}

// MARK: - Inner Grid View Modifier

private struct NavigationInnerGridViewModifier: ViewModifier {
    let topCenter: (() -> AnyView)?
    let topTrailing: (() -> AnyView)?
    let midLeading: (() -> AnyView)?
    let bottomLeading: (() -> AnyView)?
    let bottomTrailing: (() -> AnyView)?

    func body(content: Content) -> some View {
        content
            .transformEnvironment(\.navigationInnerGridConfiguration) { config in
                // Merge new configuration with existing, prioritizing new values
                config = NavigationInnerGridConfiguration(
                    topCenter: topCenter ?? config.topCenter,
                    topTrailing: topTrailing ?? config.topTrailing,
                    midLeading: midLeading ?? config.midLeading,
                    bottomLeading: bottomLeading ?? config.bottomLeading,
                    bottomTrailing: bottomTrailing ?? config.bottomTrailing
                )
            }
    }
}

// MARK: - Type-Safe Extensions

public extension View {
    /// Customize views on the navigating inner grid view that are not already being used.
    ///
    /// This modifier sets inner grid configuration in the environment that can be read
    /// by child views that conform to CustomizableNavigatingInnerGridView.
    ///
    /// - Parameters:
    ///   - topCenter: The top center view content.
    ///   - topTrailing: The top trailing view content.
    ///   - midLeading: The mid leading view content.
    ///   - bottomLeading: The bottom leading view content.
    ///   - bottomTrailing: The bottom trailing view content.
    /// - Returns: A modified view with inner grid configuration in the environment.
    func innerGrid(
        @ViewBuilder topCenter: @escaping () -> some View = { Spacer() },
        @ViewBuilder topTrailing: @escaping () -> some View = { Spacer() },
        @ViewBuilder midLeading: @escaping () -> some View = { Spacer() },
        @ViewBuilder bottomLeading: @escaping () -> some View = { Spacer() },
        @ViewBuilder bottomTrailing: @escaping () -> some View = { Spacer() }
    ) -> some View {
        modifier(NavigationInnerGridViewModifier(
            topCenter: { AnyView(topCenter()) },
            topTrailing: { AnyView(topTrailing()) },
            midLeading: { AnyView(midLeading()) },
            bottomLeading: { AnyView(bottomLeading()) },
            bottomTrailing: { AnyView(bottomTrailing()) }
        ))
    }
}
