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
    @Environment(\.colorScheme) var colorScheme

    let lightStyleURL: URL
    let darkStyleURL: URL
    var mapViewContentInset: UIEdgeInsets = .zero
    let userLayers: [StyleLayerDefinition]

    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?

    @State private var locationManager = StaticLocationManager(initialLocation: CLLocation())
    @Binding private var camera: MapViewCamera

    public init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>,
        @MapViewContentBuilder _ makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.navigationState = navigationState
        _camera = camera
        userLayers = makeMapContent()
    }

    public var body: some View {
        MapView(
            styleURL: colorScheme == .dark ? darkStyleURL : lightStyleURL,
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
                    camera = .trackUserLocationWithCourse(zoom: 18, pitch: .fixed(45))
                }
            }

            userLayers
        }
        .mapViewContentInset(mapViewContentInset)
        .mapControls {
            // No controls
        }
        .ignoresSafeArea(.all)
    }
}

#Preview("Navigation Map View") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    return NavigationMapView(
        lightStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        darkStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12))
    )
}
