import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A navigation view that dynamically switches between portrait and landscape orientations.
public struct DynamicallyOrientingNavigationView: View {
    // TODO: Add orientation handling once the landscape view is constructed.
    @State private var orientation = UIDeviceOrientation.unknown

    let styleURL: URL
    let distanceFormatter: Formatter
    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?

    @State private var locationManager = StaticLocationManager(initialLocation: CLLocation())
    @Binding private var camera: MapViewCamera

    public init(
        styleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>,
        distanceFormatter: Formatter = MKDistanceFormatter()
        // TODO: Add a symbol builder here for custom symbols along w/ route.
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        self.distanceFormatter = distanceFormatter
        _camera = camera
    }

    public var body: some View {
        switch orientation {
        case .landscapeLeft, .landscapeRight:
            Text("TODO")
        default:
            PortraitNavigationView(
                styleURL: styleURL,
                navigationState: navigationState,
                camera: $camera,
                distanceFormatter: distanceFormatter
            )
        }
    }
}

#Preview("Portrait Navigation View (Imperial)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    return DynamicallyOrientingNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
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

    return DynamicallyOrientingNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        distanceFormatter: formatter
    )
}
