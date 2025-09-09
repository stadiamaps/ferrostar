import FerrostarCore
import FerrostarSwiftUI
import MapLibreSwiftUI
import SwiftUI

// MARK: - Navigation Map Content Insets Configuration Environment

public struct NavigationMapViewContentInsetConfiguration {
    public let bundle: NavigationMapViewContentInsetBundle

    public init(bundle: NavigationMapViewContentInsetBundle = NavigationMapViewContentInsetBundle()) {
        self.bundle = bundle
    }

    // MARK: - Convenience Accessors

    /// Get the landscape content inset using the configured bundle.
    public func getLandscapeInset(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        bundle.landscape(geometry)
    }

    /// Get the portrait content inset using the configured bundle.
    public func getPortraitInset(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        bundle.portrait(geometry)
    }

    /// Get the showcase landscape content inset using the configured bundle.
    public func getShowcaseLandscapeInset(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        bundle.showcaseLandscape(geometry)
    }

    /// Get the showcase portrait content inset using the configured bundle.
    public func getShowcasePortraitInset(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        bundle.showcasePortrait(geometry)
    }

    /// Get the dynamic content inset based on orientation using the configured bundle.
    /// This method uses navigation mode insets only.
    public func getDynamicInset(for orientation: UIDeviceOrientation,
                                geometry: GeometryProxy) -> NavigationMapViewContentInsetMode
    {
        bundle.dynamic(orientation)(geometry)
    }

    /// Get the dynamic showcase content inset based on orientation using the configured bundle.
    public func getShowcaseDynamicInset(for orientation: UIDeviceOrientation,
                                        geometry: GeometryProxy) -> NavigationMapViewContentInsetMode
    {
        switch orientation {
        case .landscapeLeft, .landscapeRight:
            bundle.showcaseLandscape(geometry)
        default:
            bundle.showcasePortrait(geometry)
        }
    }

    /// Get the dynamic content inset based on camera state and orientation using the configured bundle.
    /// This is the recommended method as it automatically chooses between navigation and showcase insets.
    public func getDynamicInsetWithCameraState(for cameraState: CameraState,
                                               orientation: UIDeviceOrientation,
                                               geometry: GeometryProxy) -> NavigationMapViewContentInsetMode
    {
        bundle.dynamicWithCameraState(cameraState, orientation: orientation)(geometry)
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
    let bundle: NavigationMapViewContentInsetBundle

    func body(content: Content) -> some View {
        content
            .environment(\.navigationMapViewContentInsetConfiguration,
                         NavigationMapViewContentInsetConfiguration(bundle: bundle))
    }
}

// MARK: - Public Extensions

public extension View {
    /// Configure navigation view map content insets with a static inset mode.
    /// This consolidates the functionality from the old NavigationMapViewModifiers.
    ///
    /// - Parameter inset: The static inset mode for the navigation map view.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    func navigationMapViewContentInset(_ inset: NavigationMapViewContentInsetMode) -> some View {
        let staticBundle = NavigationMapViewContentInsetBundle(
            landscape: { _ in inset },
            portrait: { _ in inset }
        )
        return modifier(NavigationMapViewContentInsetViewModifier(bundle: staticBundle))
    }

    /// Configure navigation view map content insets using a bundle.
    ///
    /// - Parameter bundle: The bundle containing landscape and portrait inset configurations.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    func navigationMapViewContentInset(_ bundle: NavigationMapViewContentInsetBundle) -> some View {
        modifier(NavigationMapViewContentInsetViewModifier(bundle: bundle))
    }

    /// Configure navigation view map content insets for landscape orientation.
    ///
    /// - Parameter landscape: Generate the content inset for landscape mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    func navigationMapViewContentInset(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> some View {
        let bundle = NavigationMapViewContentInsetBundle(
            landscape: landscape,
            portrait: { .portrait(within: $0) }
        )
        return modifier(NavigationMapViewContentInsetViewModifier(bundle: bundle))
    }

    /// Configure navigation view map content insets for portrait orientation.
    ///
    /// - Parameter portrait: Generate the content inset for portrait mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    func navigationMapViewContentInset(
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode
    ) -> some View {
        let bundle = NavigationMapViewContentInsetBundle(
            landscape: { .landscape(within: $0) },
            portrait: portrait
        )
        return modifier(NavigationMapViewContentInsetViewModifier(bundle: bundle))
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
        let bundle = NavigationMapViewContentInsetBundle(
            landscape: landscape,
            portrait: portrait
        )
        return modifier(NavigationMapViewContentInsetViewModifier(bundle: bundle))
    }

    // MARK: - With Showcase

    /// Configure navigation view map content insets for all modes (navigation and showcase, landscape and portrait).
    ///
    /// - Parameters:
    ///   - navigationLandscape: Generate the content inset for navigation landscape mode with a given geometry proxy.
    ///   - navigationPortrait: Generate the content inset for navigation portrait mode with a given geometry proxy.
    ///   - showcaseLandscape: Generate the content inset for showcase landscape mode with a given geometry proxy.
    ///   - showcasePortrait: Generate the content inset for showcase portrait mode with a given geometry proxy.
    /// - Returns: A modified view with navigation map content insets configuration in the environment.
    func navigationMapViewContentInset(
        navigationLandscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode,
        navigationPortrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode,
        // TODO: Both of these should use the ViewComplementingSafeAreaView insets, but that needs to be a view modifier first.
        showcaseLandscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode = { _ in .edgeInset(.init(
            top: 16,
            left: 256,
            bottom: 16,
            right: 16
        )) },
        showcasePortrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode = { _ in .edgeInset(.init(
            top: 160,
            left: 16,
            bottom: 128,
            right: 16
        )) }
    ) -> some View {
        let bundle = NavigationMapViewContentInsetBundle(
            landscape: navigationLandscape,
            portrait: navigationPortrait,
            showcaseLandscape: showcaseLandscape,
            showcasePortrait: showcasePortrait
        )
        return modifier(NavigationMapViewContentInsetViewModifier(bundle: bundle))
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
