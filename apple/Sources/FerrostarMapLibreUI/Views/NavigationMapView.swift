import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

// TODO: Move to MapLibre SwiftUI (DO NOT MERGE!)
extension [StyleLayerDefinition]: StyleLayerCollection {
    public var layers: [StyleLayerDefinition] { self }
}

/// The most generic map view in Ferrostar.
///
/// This view includes renders a route line and includes a default camera.
/// It does not include other UI elements like instruction banners.
/// This is the basis of higher level views like
/// ``DynamicallyOrientingNavigationView``.
public struct NavigationMapView: View {
    let styleURL: URL
    var mapViewContentInset: UIEdgeInsets = .zero
    let activity: MapActivity
    var onStyleLoaded: (MLNStyle) -> Void
    var routeLayerOverride: ((NavigationState?) -> any StyleLayerCollection)?
    private var userLayers: (NavigationState?) -> any StyleLayerCollection

    // TODO: Configurable camera and user "puck" rotation modes
    private let navigationState: NavigationState?

    @State private var locationManager = StaticLocationManager(initialLocation: CLLocation())

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
    @available(*, deprecated, message: "Use init with content that captures navigation state")
    public init(
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        navigationState: NavigationState?,
        activity: MapActivity = .standard,
        onStyleLoaded: @escaping ((MLNStyle) -> Void),
        @MapViewContentBuilder _ makeMapContent: @escaping () -> [StyleLayerDefinition]
    ) {
        self.styleURL = styleURL
        _camera = camera
        self.navigationState = navigationState
        self.onStyleLoaded = onStyleLoaded
        userLayers = { _ in makeMapContent() }
        self.activity = activity
    }

    /// Initialize a map view tuned for turn by turn navigation.
    ///
    /// - Parameters:
    ///   - styleURL: The map's style url.
    ///   - camera: The camera binding that represents the current camera on the map.
    ///   - navigationState: The current ferrostar navigation state provided by ferrostar core.
    ///   - activity: The MapView's activity. This handles internal scaling for CarPlay or device rendering.
    ///   - routeLayerOverride: override the route layer. This can also be done using
    /// ``navigationMapViewRoute(content:)``
    ///   - onStyleLoaded: The map's style has loaded and the camera can be manipulated (e.g. to user tracking).
    ///   - mapContent: Custom maplibre symbols to display on the map view. This does not impact the route.
    public init(
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        navigationState: NavigationState?,
        activity: MapActivity = .standard,
        routeLayerOverride: ((NavigationState?) -> any StyleLayerCollection)? = nil,
        onStyleLoaded: @escaping ((MLNStyle) -> Void),
        @MapViewContentBuilder mapContent: @escaping (NavigationState?) -> some StyleLayerCollection = { _ in [] }
    ) {
        self.styleURL = styleURL
        _camera = camera
        self.navigationState = navigationState
        self.onStyleLoaded = onStyleLoaded
        self.routeLayerOverride = routeLayerOverride
        userLayers = { state in mapContent(state).layers }
        self.activity = activity
    }

    public var body: some View {
        MapView(
            styleURL: styleURL,
            camera: $camera,
            locationManager: locationManager,
            activity: activity
        ) {
            // TODO: Create logic and style for route previews. Unless ferrostarCore will handle this internally.
            routeLayer

            updateCameraIfNeeded()

            // Overlay any additional user layers.
            userLayers(navigationState)
        }
        .mapViewContentInset(mapViewContentInset)
        .mapControls {
            // No controls
        }
        .onStyleLoaded(onStyleLoaded)
        .ignoresSafeArea(.all)
    }

    private func updateCameraIfNeeded() {
        if let userLocation = navigationState?.preferredUserLocation,
           // There is no reason to push an update if the coordinate and heading are the same.
           // That's all that gets displayed, so it's all that MapLibre should care about.
           locationManager.lastLocation.coordinate != userLocation.coordinates
           .clLocationCoordinate2D || locationManager.lastLocation.course != userLocation.clLocation.course
        {
            locationManager.lastLocation = userLocation.clLocation
        }
    }

    @MapViewContentBuilder private var routeLayer: any StyleLayerCollection {
        if let routeLayerOverride {
            routeLayerOverride(navigationState)
        } else {
            if let routePolyline = navigationState?.routePolyline {
                RouteStyleLayer(polyline: routePolyline,
                                identifier: "route-polyline",
                                style: TravelledRouteStyle())
            }

            if let remainingRoutePolyline = navigationState?.remainingRoutePolyline {
                RouteStyleLayer(polyline: remainingRoutePolyline,
                                identifier: "remaining-route-polyline")
            }
        }
    }
}

#Preview("Navigation Map View") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    if case let .navigating(_, _, snappedUserLocation: userLocation, _, _, _, _, _, _, _, _) = state.tripState {
        NavigationMapView(
            styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
            camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
            navigationState: state,
            onStyleLoaded: { _ in }
        )
    } else {
        EmptyView()
    }
}
