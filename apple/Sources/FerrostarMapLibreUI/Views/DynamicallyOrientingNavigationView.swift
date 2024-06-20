import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A navigation view that dynamically switches between portrait and landscape orientations.
public struct DynamicallyOrientingNavigationView<
    TopCenter: View,
    TopTrailing: View,
    MidLeading: View,
    BottomTrailing: View
>: View {
    // TODO: Add orientation handling once the landscape view is constructed.
    @State private var orientation = UIDeviceOrientation.unknown

    let theme: any FerrostarTheme

    let styleURL: URL
    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?
    private let userLayers: [StyleLayerDefinition]

    @ViewBuilder var topCenter: () -> TopCenter
    @ViewBuilder var topTrailing: () -> TopTrailing
    @ViewBuilder var midLeading: () -> MidLeading
    @ViewBuilder var bottomTrailing: () -> BottomTrailing

    @Binding var camera: MapViewCamera
    @Binding var snappedZoom: Double
    @Binding var useSnappedCamera: Bool

    var onTapExit: () -> Void

    /// Initialize a map view tuned for turn by turn navigation.
    ///
    /// - Parameters:
    ///   - styleURL: The style URL for the map. This can dynamically change between light and dark mode.
    ///   - navigationState: The ferrostar navigations state. This is used primarily to drive user location on the map.
    ///   - camera: The camera which is controlled by the navigation state, but may also be pushed to for other cases
    /// (e.g. user pan).
    ///   - snappedZoom: The zoom for the snapped camera. This can be fixed, customized or controlled by the camera.
    ///   - useSnappedCamera: Whether to use the ferrostar snapped camera or the camer binding itself.
    ///   - distanceFormatter: The formatter for distances in instruction views.
    public init(
        theme: any FerrostarTheme = DefaultFerrostarTheme(),
        styleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>,
        snappedZoom: Binding<Double>,
        useSnappedCamera: Binding<Bool>,
        onTapExit: @escaping () -> Void = {},
        @MapViewContentBuilder mapContent: () -> [StyleLayerDefinition] = { [] },
        @ViewBuilder topCenter: @escaping () -> TopCenter = { InfiniteSpacer() },
        @ViewBuilder topTrailing: @escaping () -> TopTrailing = { InfiniteSpacer() },
        @ViewBuilder midLeading: @escaping () -> MidLeading = { InfiniteSpacer() },
        @ViewBuilder bottomTrailing: @escaping () -> BottomTrailing = { InfiniteSpacer() }
    ) {
        self.theme = theme
        self.styleURL = styleURL
        self.navigationState = navigationState
        userLayers = mapContent()
        self.onTapExit = onTapExit

        self.topCenter = topCenter
        self.topTrailing = topTrailing
        self.midLeading = midLeading
        self.bottomTrailing = bottomTrailing

        _camera = camera
        _snappedZoom = snappedZoom
        _useSnappedCamera = useSnappedCamera
    }

    public var body: some View {
        switch orientation {
        case .landscapeLeft, .landscapeRight:
            Text("TODO")
        default:
            PortraitNavigationView(
                theme: theme,
                styleURL: styleURL,
                navigationState: navigationState,
                camera: $camera,
                snappedZoom: $snappedZoom,
                useSnappedCamera: $useSnappedCamera,
                onTapExit: onTapExit,
                mapContent: { userLayers },
                topCenter: topCenter,
                topTrailing: topTrailing,
                midLeading: midLeading,
                bottomTrailing: bottomTrailing
            )
        }
    }
}

#Preview("Portrait Navigation View (Imperial)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    let theme = DefaultFerrostarTheme(distanceFormatter: formatter)

    return DynamicallyOrientingNavigationView(
        theme: theme,
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true)
    )
}

#Preview("Portrait Navigation View (Metric)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .metric

    let theme = DefaultFerrostarTheme(distanceFormatter: formatter)

    return DynamicallyOrientingNavigationView(
        theme: theme,
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true)
    )
}
