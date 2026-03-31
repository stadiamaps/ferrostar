import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// The most generic map view in Ferrostar.
///
/// This view includes renders a route line and includes a default camera.
/// It does not include other UI elements like instruction banners.
/// This is the basis of higher level views like
/// ``DynamicallyOrientingNavigationView``.
public struct NavigationMapView: View {
    @Environment(\.navigationMapViewRouteOverlayConfiguration) private var routeConfig
    @Environment(\.navigationMapViewContentInsetConfiguration) private var contentInsetConfig

    let styleURL: URL
    var mapViewContentInset: UIEdgeInsets = .zero
    var onStyleLoaded: (MLNStyle) -> Void
    var onUserTrackingModeChanged: (MLNUserTrackingMode, Bool) -> Void
    let userLayers: [StyleLayerDefinition]
    let activity: MapActivity

    // TODO: Configurable camera and user "puck" rotation modes

    private let navigationState: NavigationState?

    @State private var nonNavigatingLocationManager: (any MLNLocationManager)?
    @State private var navigatingLocationManager: any NavigationDrivenLocationManager

    // MARK: Camera Settings

    @Binding var camera: MapViewCamera

    /// Initialize a map view tuned for turn by turn navigation.
    ///
    /// - Parameters:
    ///   - styleURL: The map's style url.
    ///   - camera: The camera binding that represents the current camera on the map.
    ///   - navigationState: The current ferrostar navigation state provided by ferrostar core.
    ///   - onStyleLoaded: The map's style has loaded and the camera can be manipulated (e.g. to user tracking).
    ///   - makeMapContent: Custom maplibre symbols to display on the map view.
    public init(
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        navigationState: NavigationState?,
        locationManagerConfiguration: NavigationLocationManagerConfiguration = .default,
        activity: MapActivity = .standard,
        onUserTrackingModeChanged: @escaping (MLNUserTrackingMode, Bool) -> Void = { _, _ in },
        onStyleLoaded: @escaping ((MLNStyle) -> Void),
        @MapViewContentBuilder _ makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.styleURL = styleURL
        _camera = camera
        self.navigationState = navigationState
        self.onUserTrackingModeChanged = onUserTrackingModeChanged
        self.onStyleLoaded = onStyleLoaded
        userLayers = makeMapContent()
        self.activity = activity
        _nonNavigatingLocationManager = State(initialValue: locationManagerConfiguration.nonNavigatingLocationManager)
        _navigatingLocationManager = State(initialValue: locationManagerConfiguration.navigatingLocationManager)
    }

    public var body: some View {
        GeometryReader { geometry in
            MapView(
                styleURL: styleURL,
                camera: $camera,
                locationManager: activeLocationManager,
                activity: activity
            ) {
                // TODO: Create logic and style for route previews. Unless ferrostarCore will handle this internally.
                routeConfig.routeOverlay(navigationState)

                updateCameraIfNeeded()

                // Overlay any additional user layers.
                userLayers
            }
            .mapContentInset(calculatedMapViewInsets(for: geometry).uiEdgeInsets)
            .mapControls {
                // No controls
            }
            .onMapUserTrackingModeChanged(onUserTrackingModeChanged)
            .onMapStyleLoaded(onStyleLoaded)
            .ignoresSafeArea(.all)
        }
    }

    private func calculatedMapViewInsets(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        if navigationState?.isNavigating == true {
            return contentInsetConfig.bundle
                .dynamicWithCameraState(camera.state, isLandscape: geometry.isLandscape)(geometry)
        }

        switch camera.state {
        case .rect, .showcase:
            if geometry.isLandscape {
                return contentInsetConfig.getShowcaseLandscapeInset(for: geometry)
            } else {
                return contentInsetConfig.getShowcasePortraitInset(for: geometry)
            }
        default:
            return .edgeInset(UIEdgeInsets(
                top: geometry.safeAreaInsets.top,
                left: geometry.safeAreaInsets.leading,
                bottom: geometry.safeAreaInsets.bottom,
                right: geometry.safeAreaInsets.trailing
            ))
        }
    }

    private func updateCameraIfNeeded() {
        if let userLocation = navigationState?.preferredUserLocation,
           // There is no reason to push an update if the coordinate and heading are the same.
           // That's all that gets displayed, so it's all that MapLibre should care about.
           navigatingLocationManager.lastLocation.coordinate != userLocation.coordinates
           .clLocationCoordinate2D || navigatingLocationManager.lastLocation.course != userLocation.clLocation.course
        {
            navigatingLocationManager.lastLocation = userLocation.clLocation
        }
    }

    private var activeLocationManager: (any MLNLocationManager)? {
        if navigationState?.isNavigating == true {
            return navigatingLocationManager
        }
        return nonNavigatingLocationManager
    }
}

#Preview("Navigation Map View") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    guard case let .navigating(_, _, snappedUserLocation: userLocation, _, _, _, _, _, _, _, _) = state.tripState else {
        return EmptyView()
    }

    return NavigationMapView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
        navigationState: state,
        onStyleLoaded: { _ in }
    )
}
