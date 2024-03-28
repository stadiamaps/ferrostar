import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public struct NavigationMapView: View {
    @Environment(\.colorScheme) var colorScheme

    let lightStyleURL: URL
    let darkStyleURL: URL
    let distanceFormatter: Formatter
    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?

    @State private var locationManager = StaticLocationManager(initialLocation: CLLocation())
    @Binding private var camera: MapViewCamera

    public init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>,
        distanceFormatter: Formatter = MKDistanceFormatter()
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.navigationState = navigationState
        self.distanceFormatter = distanceFormatter
        _camera = camera
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
                InstructionsView(
                    visualInstruction: visualInstructions,
                    distanceFormatter: distanceFormatter,
                    distanceToNextManeuver: navigationState.distanceToNextManeuver
                )
            }
        })
    }
}

#Preview("Navigation Map View (Imperial)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    return NavigationMapView(
        lightStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        darkStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        distanceFormatter: formatter
    )
}

#Preview("Navigation Map View (Metric)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .metric

    return NavigationMapView(
        lightStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        darkStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        distanceFormatter: formatter
    )
}
