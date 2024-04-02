import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A portrait orientation navigation view that includes the InstructionsView at the top.
public struct PortraitNavigationView: View {
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
        // TODO: Add a symbol builder here for custom symbols along w/ route.
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.navigationState = navigationState
        self.distanceFormatter = distanceFormatter
        _camera = camera
    }

    public var body: some View {
        NavigationMapView(
            lightStyleURL: lightStyleURL,
            darkStyleURL: darkStyleURL,
            navigationState: navigationState,
            camera: $camera
        )
        .navigationMapViewContentInset(.portrait)
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

#Preview("Portrait Navigation View (Imperial)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    return PortraitNavigationView(
        lightStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        darkStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        distanceFormatter: formatter
    )
}

#Preview("Portrait Navigation View (Metric)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .metric

    return PortraitNavigationView(
        lightStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        darkStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        distanceFormatter: formatter
    )
}
