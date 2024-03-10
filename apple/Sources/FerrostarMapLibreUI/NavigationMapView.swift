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

    public init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: NavigationState?,
        initialCamera: MapViewCamera
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.navigationState = navigationState
        _camera = .constant(initialCamera)
        // TODO: Set up following of the user
    }

    public var body: some View {
        MapView(
            styleURL: colorScheme == .dark ? darkStyleURL : lightStyleURL,
            camera: $camera
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
                let userLocationSource = ShapeSource(identifier: "user-location-source") {
                    MLNPointFeature(coordinate: snappedLocation.coordinates.clLocationCoordinate2D)
                }

                SymbolStyleLayer(identifier: "user-location", source: userLocationSource)
                    .iconImage(UIImage(systemName: "location.north.circle.fill")!)
                    .iconRotation(Double(navigationState?.snappedLocation.courseOverGround?.degrees ?? 0))
            }
        }
        .edgesIgnoringSafeArea(.all)
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
        initialCamera: .default()
    )
}
