import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A portrait orientation navigation view that includes the InstructionsView at the top.
public struct PortraitNavigationView: View, CustomizableNavigatingInnerGridView, SpeedLimitViewHost,
    CurrentRoadNameViewHost
{
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let styleURL: URL
    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?
    private let userLayers: [StyleLayerDefinition]

    public var speedLimit: Measurement<UnitSpeed>?
    public var speedLimitStyle: SpeedLimitView.SignageStyle?

    public var topCenter: (() -> AnyView)?
    public var topTrailing: (() -> AnyView)?
    public var midLeading: (() -> AnyView)?
    public var bottomTrailing: (() -> AnyView)?

    public var minimumSafeAreaInsets: EdgeInsets

    @Binding var camera: MapViewCamera
    let navigationCamera: MapViewCamera
    public var currentRoadNameView: AnyView?

    let isMuted: Bool
    let onTapMute: () -> Void
    var onTapExit: (() -> Void)?

    /// Create a portrait navigation view. This view is optimized for display on a portrait screen where the
    /// instructions and trip progress view are on the top and bottom of the screen.
    /// The user puck and route are optimized for the center of the screen.
    ///
    /// - Parameters:
    ///   - styleURL: The map's style url.
    ///   - camera: The camera binding that represents the current camera on the map.
    ///   - navigationCamera: The default navigation camera. This sets the initial camera & is also used when the center
    ///                       on user button it tapped.
    ///   - navigationState: The current ferrostar navigation state provided by the Ferrostar core.
    ///   - minimumSafeAreaInsets: The minimum padding to apply from safe edges. See `complementSafeAreaInsets`.
    ///   - onTapExit: An optional behavior to run when the ``TripProgressView`` exit button is tapped. When nil
    /// (default) the
    /// exit button is hidden.
    ///   - makeMapContent: Custom maplibre symbols to display on the map view.
    public init(
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        navigationCamera: MapViewCamera = .automotiveNavigation(),
        navigationState: NavigationState?,
        isMuted: Bool,
        minimumSafeAreaInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
        onTapMute: @escaping () -> Void,
        onTapExit: (() -> Void)? = nil,
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        self.isMuted = isMuted
        self.minimumSafeAreaInsets = minimumSafeAreaInsets
        self.onTapMute = onTapMute
        self.onTapExit = onTapExit

        userLayers = makeMapContent()

        _camera = camera
        self.navigationCamera = navigationCamera
        currentRoadNameView = AnyView(CurrentRoadNameView(currentRoadName: navigationState?.currentRoadName))
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                NavigationMapView(
                    styleURL: styleURL,
                    camera: $camera,
                    navigationState: navigationState,
                    onStyleLoaded: { _ in
                        camera = navigationCamera
                    }
                ) {
                    userLayers
                }
                .navigationMapViewContentInset(.portrait(within: geometry))

                PortraitNavigationOverlayView(
                    navigationState: navigationState,
                    speedLimit: speedLimit,
                    speedLimitStyle: speedLimitStyle,
                    isMuted: isMuted,
                    showMute: true,
                    onMute: onTapMute,
                    showZoom: true,
                    onZoomIn: { camera.incrementZoom(by: 1) },
                    onZoomOut: { camera.incrementZoom(by: -1) },
                    showCentering: !camera.isTrackingUserLocationWithCourse,
                    onCenter: { camera = navigationCamera },
                    onTapExit: onTapExit,
                    currentRoadNameView: currentRoadNameView
                )
                .innerGrid {
                    topCenter?()
                } topTrailing: {
                    topTrailing?()
                } midLeading: {
                    midLeading?()
                } bottomTrailing: {
                    bottomTrailing?()
                }.complementSafeAreaInsets(parentGeometry: geometry, minimumInsets: minimumSafeAreaInsets)
            }
        }
    }
}

#Preview("Portrait Navigation View (Imperial)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    guard case let .navigating(_, snappedUserLocation: userLocation, _, _, _, _, _, _, _) = state.tripState else {
        return EmptyView()
    }

    return PortraitNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
        navigationState: state,
        isMuted: true,
        onTapMute: {}
    )
    .navigationFormatterCollection(FoundationFormatterCollection(distanceFormatter: formatter))
}

#Preview("Portrait Navigation View (Metric)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .metric

    guard case let .navigating(_, snappedUserLocation: userLocation, _, _, _, _, _, _, _) = state.tripState else {
        return EmptyView()
    }

    return PortraitNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
        navigationState: state,
        isMuted: true,
        onTapMute: {}
    )
    .navigationFormatterCollection(FoundationFormatterCollection(distanceFormatter: formatter))
}
