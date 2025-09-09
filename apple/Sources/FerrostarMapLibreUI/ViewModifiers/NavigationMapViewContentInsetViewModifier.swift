import FerrostarCore
import FerrostarSwiftUI
import SwiftUI

// MARK: - Navigation Map Content Insets Configuration Environment

public struct NavigationMapViewContentInsetConfiguration {
    public let landscape: ((GeometryProxy) -> NavigationMapViewContentInsetMode)?
    public let portrait: ((GeometryProxy) -> NavigationMapViewContentInsetMode)?

    public init(
        landscape: ((GeometryProxy) -> NavigationMapViewContentInsetMode)? = nil,
        portrait: ((GeometryProxy) -> NavigationMapViewContentInsetMode)? = nil
    ) {
        self.landscape = landscape
        self.portrait = portrait
    }

    // MARK: - Convenience Accessors

    /// Get the landscape content inset, falling back to default if none is configured.
    public func getLandscapeInset(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        if let landscapeInset = landscape {
            landscapeInset(geometry)
        } else {
            NavigationMapViewContentInsetBundle().landscape(geometry)
        }
    }

    /// Get the portrait content inset, falling back to default if none is configured.
    public func getPortraitInset(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        if let portraitInset = portrait {
            portraitInset(geometry)
        } else {
            NavigationMapViewContentInsetBundle().portrait(geometry)
        }
    }

    /// Get the dynamic content inset based on orientation, falling back to defaults if none is configured.
    public func getDynamicInset(for orientation: UIDeviceOrientation,
                                geometry: GeometryProxy) -> NavigationMapViewContentInsetMode
    {
        switch orientation {
        case .landscapeLeft, .landscapeRight:
            getLandscapeInset(for: geometry)
        default:
            getPortraitInset(for: geometry)
        }
    }
}

private struct NavigationMapViewContentInsetConfigurationKey: EnvironmentKey {
    static var defaultValue: NavigationMapViewContentInsetConfiguration = .init()
}

public extension EnvironmentValues {
    var navigationMapViewContentInsetConfiguration: NavigationMapViewContentInsetConfiguration {
        get { self[NavigationMapViewContentInsetConfigurationKey.self] }
        set { self[NavigationMapViewContentInsetConfigurationKey.self] = newValue }
    }
}

// MARK: - Navigation Map Content Insets Modifier

private struct NavigationMapViewContentInsetViewModifier: ViewModifier {
    let landscape: ((GeometryProxy) -> NavigationMapViewContentInsetMode)?
    let portrait: ((GeometryProxy) -> NavigationMapViewContentInsetMode)?

    func body(content: Content) -> some View {
        content
            .transformEnvironment(\.navigationMapViewContentInsetConfiguration) { config in
                // Merge new configuration with existing, prioritizing new values
                config = NavigationMapViewContentInsetConfiguration(
                    landscape: landscape ?? config.landscape,
                    portrait: portrait ?? config.portrait
                )
            }
    }
}

// MARK: - Type-Safe Extensions

public extension View {
    /// Configure navigation view map content insets for landscape orientation.
    ///
    /// - Parameter landscape: Generate the content inset for landscape mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    func navigationMapViewContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> some View {
        modifier(NavigationMapViewContentInsetViewModifier(
            landscape: landscape,
            portrait: nil
        ))
    }

    /// Configure navigation view map content insets for portrait orientation.
    ///
    /// - Parameter portrait: Generate the content inset for portrait mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    func navigationMapViewContentInset(
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> some View {
        modifier(NavigationMapViewContentInsetViewModifier(
            landscape: nil,
            portrait: portrait
        ))
    }

    /// Configure navigation view map content insets for both landscape and portrait orientations.
    ///
    /// - Parameters:
    ///   - landscape: Generate the content inset for landscape mode with a given geometry proxy.
    ///   - portrait: Generate the content inset for portrait mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    func navigationMapViewContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode,
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> some View {
        modifier(NavigationMapViewContentInsetViewModifier(
            landscape: landscape,
            portrait: portrait
        ))
    }

    // MARK: - Backwards Compatible navigationView Prefixed Methods

    /// Configure navigation view map content insets for landscape orientation.
    /// This is a backwards-compatible wrapper for navigationMapViewContentInset(landscape:).
    ///
    /// - Parameter landscape: Generate the content inset for landscape mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    @available(*, deprecated, renamed: "navigationMapViewContentInset")
    func navigationViewMapContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> some View {
        navigationMapViewContentInset(landscape: landscape)
    }

    /// Configure navigation view map content insets for portrait orientation.
    /// This is a backwards-compatible wrapper for navigationMapViewContentInset(portrait:).
    ///
    /// - Parameter portrait: Generate the content inset for portrait mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    @available(*, deprecated, renamed: "navigationMapViewContentInset")
    func navigationViewMapContentInset(
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> some View {
        navigationMapViewContentInset(portrait: portrait)
    }

    /// Configure navigation view map content insets for both landscape and portrait orientations.
    /// This is a backwards-compatible wrapper for navigationMapViewContentInset(landscape:portrait:).
    ///
    /// - Parameters:
    ///   - landscape: Generate the content inset for landscape mode with a given geometry proxy.
    ///   - portrait: Generate the content inset for portrait mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    @available(*, deprecated, renamed: "navigationMapViewContentInset")
    func navigationViewMapContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode,
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> some View {
        navigationMapViewContentInset(landscape: landscape, portrait: portrait)
    }
}
