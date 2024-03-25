import FerrostarCore
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public struct NavigationMapView: View {
    @Environment(\.colorScheme) var colorScheme

    let lightStyleURL: URL
    let darkStyleURL: URL
    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?

    @State private var locationManager = StaticLocationManager(initialLocation: CLLocation())
    @Binding private var camera: MapViewCamera

    public init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.navigationState = navigationState
        _camera = camera
        // TODO: Set up following of the user
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
        }
        // TODO: make this more configurable / adaptable
        .mapViewContentInset(UIEdgeInsets(top: 450, left: 0, bottom: 0, right: 0))
        .mapControls {
            // No controls
        }
        .ignoresSafeArea(.all)
        .overlay(alignment: .top, content: {
            if let navigationState,
               let visualInstructions = navigationState.visualInstructions
            {
                BannerView(instructions: visualInstructions,
                           distanceToNextManeuver: navigationState.distanceToNextManeuver)
            }
        })
    }
}

#Preview {
    // TODO: Make map URL configurable but gitignored
    NavigationMapView(
        lightStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        darkStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: .modifiedPedestrianExample(droppingNWaypoints: 4),
        camera: .constant(.default())
    )
}
