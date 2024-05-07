import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A portrait orientation navigation view that includes the InstructionsView at the top.
public struct PortraitNavigationView: View {
    let styleURL: URL
    let distanceFormatter: Formatter
    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?

    @State private var locationManager = StaticLocationManager(initialLocation: CLLocation())
    @Binding var camera: MapViewCamera
    @Binding var snappedZoom: Double
    @Binding var useSnappedCamera: Bool

    public init(
        styleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>,
        snappedZoom: Binding<Double>,
        useSnappedCamera: Binding<Bool>,
        distanceFormatter: Formatter = MKDistanceFormatter()
        // TODO: Add a symbol builder here for custom symbols along w/ route.
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        self.distanceFormatter = distanceFormatter
        _camera = camera
        _snappedZoom = snappedZoom
        _useSnappedCamera = useSnappedCamera
    }

    public var body: some View {
        GeometryReader { geometry in
            NavigationMapView(
                styleURL: styleURL,
                navigationState: navigationState,
                camera: $camera,
                snappedZoom: $snappedZoom,
                useSnappedCamera: $useSnappedCamera
            )
            .navigationMapViewContentInset(.portrait(within: geometry))
            .overlay(alignment: .top, content: {
                if let navigationState,
                   let visualInstructions = navigationState.visualInstruction
                {
                    InstructionsView(
                        visualInstruction: visualInstructions,
                        distanceFormatter: distanceFormatter,
                        distanceToNextManeuver: navigationState.progress?.distanceToNextManeuver
                    )
                }
            })
        }
    }
}

#Preview("Portrait Navigation View (Imperial)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    return PortraitNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true),
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
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true),
        distanceFormatter: formatter
    )
}
