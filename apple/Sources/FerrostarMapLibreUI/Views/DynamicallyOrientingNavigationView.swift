import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A navigation view that dynamically switches between portrait and landscape orientations.
public struct DynamicallyOrientingNavigationView: View {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection
    @Environment(\.navigationViewComponentsConfiguration) private var componentsConfig

    let styleURL: URL
    @Binding var camera: MapViewCamera
    let navigationCamera: MapViewCamera
    let locationManagerConfiguration: NavigationLocationManagerConfiguration?

    private let navigationState: NavigationState?
    private let userLayers: [StyleLayerDefinition]
    @State private var userTrackingMode: MLNUserTrackingMode = .followWithCourse

    // Speed limit and grid configuration now read from environment to avoid struct copying issues
    @Environment(\.speedLimitConfiguration) private var speedLimitConfig
    @Environment(\.navigationInnerGridConfiguration) private var gridConfig

    let isMuted: Bool
    let onTapMute: () -> Void
    var onTapExit: (() -> Void)?

    public var minimumSafeAreaInsets: EdgeInsets

    /// Create a dynamically orienting navigation view. This view automatically arranges child views for both portrait
    /// and landscape orientations.
    ///
    /// - Parameters:
    ///   - styleURL: The map's style url.
    ///   - camera: The camera binding that represents the current camera on the map.
    ///   - navigationCamera: The default navigation camera. This sets the initial camera & is also used when the center
    ///         on user button it tapped.
    ///   - navigationState: The current ferrostar navigation state provided by the Ferrostar core.
    ///   - minimumSafeAreaInsets: The minimum padding to apply from safe edges. See `complementSafeAreaInsets`.
    ///   - onTapExit: An optional behavior to run when the ``TripProgressView`` exit button is tapped. When nil
    /// (default) the
    /// exit button is hidden.
    ///   - makeMapContent: Custom maplibre symbols to display on the map view.
    public init(
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        navigationCamera: MapViewCamera = .automotiveNavigation(),
        navigationState: NavigationState?,
        locationManagerConfiguration: NavigationLocationManagerConfiguration? = nil,
        isMuted: Bool,
        minimumSafeAreaInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
        onTapMute: @escaping () -> Void,
        onTapExit: (() -> Void)? = nil,
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        self.locationManagerConfiguration = locationManagerConfiguration
        self.isMuted = isMuted
        self.minimumSafeAreaInsets = minimumSafeAreaInsets
        self.onTapMute = onTapMute
        self.onTapExit = onTapExit

        userLayers = makeMapContent()

        _camera = camera
        self.navigationCamera = navigationCamera
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                NavigationMapView(
                    styleURL: styleURL,
                    camera: $camera,
                    navigationState: navigationState,
                    locationManagerConfiguration: locationManagerConfiguration,
                    onUserTrackingModeChanged: { mode, _ in
                        userTrackingMode = mode
                    },
                    onStyleLoaded: { _ in
                        if isNavigating {
                            camera = navigationCamera
                        }
                    }
                ) {
                    userLayers
                }

                if geometry.isLandscape {
                    LandscapeNavigationOverlayView(
                        navigationState: navigationState,
                        speedLimit: speedLimitConfig.speedLimit,
                        speedLimitStyle: speedLimitConfig.speedLimitStyle,
                        isMuted: isMuted,
                        showMute: navigationState?.isNavigating == true,
                        onMute: onTapMute,
                        showZoom: isNavigating,
                        onZoomIn: { camera.incrementZoom(by: 1) },
                        onZoomOut: { camera.incrementZoom(by: -1) },
                        cameraControlState: cameraControlState,
                        onTapExit: onTapExit
                    )
                    .navigationViewInnerGrid {
                        gridConfig.getTopCenter()
                    } topTrailing: {
                        gridConfig.getTopTrailing()
                    } midLeading: {
                        gridConfig.getMidLeading()
                    } bottomLeading: {
                        gridConfig.getBottomLeading()
                    } bottomTrailing: {
                        gridConfig.getBottomTrailing()
                    }.complementSafeAreaInsets(parentGeometry: geometry, minimumInsets: minimumSafeAreaInsets)
                } else {
                    PortraitNavigationOverlayView(
                        navigationState: navigationState,
                        speedLimit: speedLimitConfig.speedLimit,
                        speedLimitStyle: speedLimitConfig.speedLimitStyle,
                        isMuted: isMuted,
                        showMute: navigationState?.isNavigating == true,
                        onMute: onTapMute,
                        showZoom: isNavigating,
                        onZoomIn: { camera.incrementZoom(by: 1) },
                        onZoomOut: { camera.incrementZoom(by: -1) },
                        cameraControlState: cameraControlState,
                        onTapExit: onTapExit
                    )
                    .navigationViewInnerGrid {
                        gridConfig.getTopCenter()
                    } topTrailing: {
                        gridConfig.getTopTrailing()
                    } midLeading: {
                        gridConfig.getMidLeading()
                    } bottomLeading: {
                        gridConfig.getBottomLeading()
                    } bottomTrailing: {
                        gridConfig.getBottomTrailing()
                    }.complementSafeAreaInsets(parentGeometry: geometry, minimumInsets: minimumSafeAreaInsets)
                }
            }
        }
    }

    private var isNavigating: Bool {
        navigationState?.isNavigating == true
    }

    private var cameraControlState: CameraControlState {
        NavigationCameraControlResolver(
            isNavigating: isNavigating,
            camera: camera,
            userTrackingMode: userTrackingMode,
            navigationCamera: navigationCamera,
            routeOverviewCamera: navigationState?.routeOverviewCamera,
            setCamera: { camera = $0 }
        )
        .cameraControlState()
    }
}

#Preview("Portrait Navigation View (Imperial)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    guard case let .navigating(_, _, snappedUserLocation: userLocation, _, _, _, _, _, _, _, _) = state.tripState else {
        return EmptyView()
    }

    return DynamicallyOrientingNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
        navigationState: state,
        isMuted: true,
        onTapMute: {}
    )
    .navigationFormatterCollection(FoundationFormatterCollection(distanceFormatter: formatter))
}

#Preview("Portrait Navigation View (Metric)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .metric

    guard case let .navigating(_, _, snappedUserLocation: userLocation, _, _, _, _, _, _, _, _) = state.tripState else {
        return EmptyView()
    }

    return DynamicallyOrientingNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
        navigationState: state,
        isMuted: true,
        onTapMute: {}
    )
    .navigationFormatterCollection(FoundationFormatterCollection(distanceFormatter: formatter))
}
