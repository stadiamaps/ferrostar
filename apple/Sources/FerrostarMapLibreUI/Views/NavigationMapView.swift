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
    let styleURL: URL
    var mapViewContentInset: UIEdgeInsets = .zero
    var onStyleLoaded: (MLNStyle) -> Void
    let userLayers: [StyleLayerDefinition]
    
    let mapViewModifiers: (_ view: MapView<MLNMapViewController>, _ isNavigating: Bool) -> MapView<MLNMapViewController>

    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState? 

    @State private var locationManager = StaticLocationManager(initialLocation: CLLocation())

    // MARK: Camera Settings

    @Binding var camera: MapViewCamera
    
    private var effectiveMapViewContentInset: UIEdgeInsets {
        return navigationState?.isNavigating == true ? mapViewContentInset : .zero
    }

    /// Initialize a map view tuned for turn by turn navigation.
    ///
    /// - Parameters:
    ///   - styleURL: The map's style url.
    ///   - camera: The camera binding that represents the current camera on the map.
    ///   - navigationState: The current ferrostar navigation state provided by ferrostar core.
    ///   - onStyleLoaded: The map's style has loaded and the camera can be manipulated (e.g. to user tracking).
    ///   - makeMapContent: Custom maplibre symbols to display on the map view.
    ///   - mapViewModifiers: An optional closure that allows you to apply custom view and map modifiers to the `MapView`. The closure
    ///     takes the `MapView` instance and provides a Boolean indicating if navigation is active, and returns an `AnyView`. Use this to attach onMapTapGesture and other view modifiers to the underlying MapView and customize when the modifiers are applied using
    ///       the isNavigating modifier.
    ///     By default, it returns the unmodified `MapView`.
    public init(
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        navigationState: NavigationState?,
        onStyleLoaded: @escaping ((MLNStyle) -> Void),
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] },
        mapViewModifiers: @escaping (_ view: MapView<MLNMapViewController>, _ isNavigating: Bool) -> MapView<MLNMapViewController> = { transferView, _ in
            transferView
        }
    ) {
        self.styleURL = styleURL
        _camera = camera
        self.navigationState = navigationState
        self.onStyleLoaded = onStyleLoaded
        self.userLayers = makeMapContent()
        self.mapViewModifiers = mapViewModifiers
    }

    @ViewBuilder
    public var body: some View {
        MapView(
            styleURL: styleURL,
            camera: $camera,
            locationManager: locationManager
        ) {
            // TODO: Create logic and style for route previews. Unless ferrostarCore will handle this internally.

            if let routePolyline = navigationState?.routePolyline {
                RouteStyleLayer(polyline: routePolyline,
                                identifier: "route-polyline",
                                style: TravelledRouteStyle())
            }

            if let remainingRoutePolyline = navigationState?.remainingRoutePolyline {
                RouteStyleLayer(polyline: remainingRoutePolyline,
                                identifier: "remaining-route-polyline")
            }

            updateCameraIfNeeded()

            // Overlay any additional user layers.
            userLayers
        }
        .mapViewContentInset(effectiveMapViewContentInset)
        .mapControls {
            // No controls
        }
        .onStyleLoaded(onStyleLoaded)
        .applyTransform(transform: mapViewModifiers, isNavigating: navigationState?.isNavigating == true)
        .ignoresSafeArea(.all)
    }

    private func updateCameraIfNeeded() {
        if case let .navigating(_, snappedUserLocation: userLocation, _, _, _, _, _, _, _) = navigationState?.tripState,
           // There is no reason to push an update if the coordinate and heading are the same.
           // That's all that gets displayed, so it's all that MapLibre should care about.
           locationManager.lastLocation.coordinate != userLocation.coordinates
           .clLocationCoordinate2D || locationManager.lastLocation.course != userLocation.clLocation.course
        {
            locationManager.lastLocation = userLocation.clLocation
        }
    }
}

extension MapView<MLNMapViewController> {
    @ViewBuilder
    func applyTransform<Content: View>(
        transform: (MapView<MLNMapViewController>, Bool) -> Content, isNavigating: Bool) -> some View {
        transform(self, isNavigating)
    }
}


#Preview("Navigation Map View") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    guard case let .navigating(_, snappedUserLocation: userLocation, _, _, _, _, _, _, _) = state.tripState else {
        return EmptyView()
    }

    return NavigationMapView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
        navigationState: state,
        onStyleLoaded: { _ in }
    )
}
