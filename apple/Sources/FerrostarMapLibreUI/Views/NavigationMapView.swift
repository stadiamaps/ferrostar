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
    let userLayers: [StyleLayerDefinition]

    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?

    @State private var locationManager = StaticLocationManager(initialLocation: CLLocation())

    // MARK: Camera Settings

    @Binding var camera: MapViewCamera

    /// The snapped camera zoom. This is used to override the camera zoom whenever snapping is active.
    @Binding var snappedZoom: Double

    /// Whether to snap the camera on the next navigation status update. When this is false,
    /// the user can browse the map freely.
    @Binding var useSnappedCamera: Bool

    /// The MapViewPort is used to construct the camera at the end of a drag gesture.
    @State private var mapViewPort: MapViewPort?

    /// The breakway velocity is used on the drag gesture to determine when allow a drag to
    /// disable the snapped camera (assuming it's not constant(true).
    ///
    /// Tune this value to reduce the number of accidental drags that detach the camera
    /// from the snapped user location.
    private let breakwayVelocity: CGFloat

    /// Initialize a map view tuned for turn by turn navigation.
    ///
    /// - Parameters:
    ///   - styleURL: The style URL for the map. This can dynamically change between light and dark mode.
    ///   - navigationState: The ferrostar navigations state. This is used primarily to drive user location on the map.
    ///   - camera: The camera which is controlled by the navigation state, but may also be pushed to for other cases
    /// (e.g. user pan).
    ///   - snappedZoom: The zoom for the snapped camera. This can be fixed, customized or controlled by the camera.
    ///   - useSnappedCamera: Whether to use the ferrostar snapped camera or the camer binding itself.
    ///   - snappingBreakawayVelocity: The drag gesture velocity used to disable snapping. This can be tuned to prevent
    /// accidental drags.
    ///   - content: Any additional MapLibre symbols to show on the map.
    public init(
        styleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>,
        snappedZoom: Binding<Double>,
        useSnappedCamera: Binding<Bool>,
        snappingBreakawayVelocity: CGFloat = 25,
        @MapViewContentBuilder _ makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        _camera = camera
        _snappedZoom = snappedZoom
        _useSnappedCamera = useSnappedCamera
        breakwayVelocity = snappingBreakawayVelocity
        userLayers = makeMapContent()
    }

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

            if let snappedLocation = navigationState?.snappedLocation {
                locationManager.lastLocation = snappedLocation.clLocation

                // TODO: Be less forceful about this.
                DispatchQueue.main.async {
                    if useSnappedCamera {
                        camera = .trackUserLocationWithCourse(zoom: snappedZoom,
                                                              pitch: .fixed(45))
                    }
                }
            }

            // Overlay any additional user layers.
            userLayers
        }
        .mapViewContentInset(mapViewContentInset)
        .mapControls {
            // No controls
        }
        .onMapViewPortUpdate { mapViewPort in
            self.mapViewPort = mapViewPort
        }
        .gesture(
            DragGesture()
                .onChanged { gesture in
                    guard abs(gesture.velocity.width) > breakwayVelocity
                        || abs(gesture.velocity.height) > breakwayVelocity
                    else {
                        return
                    }

                    useSnappedCamera = false
                    if let mapViewPort {
                        camera = mapViewPort.asMapViewCamera()
                    }
                }
        )
        .ignoresSafeArea(.all)
    }
}

#Preview("Navigation Map View") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    return NavigationMapView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true)
    )
}
