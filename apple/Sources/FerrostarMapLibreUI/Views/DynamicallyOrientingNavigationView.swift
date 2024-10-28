import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A navigation view that dynamically switches between portrait and landscape orientations.
public struct DynamicallyOrientingNavigationView<T: SpokenInstructionObserver & ObservableObject>: View,
    CustomizableNavigatingInnerGridView
{
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    @State private var orientation = UIDevice.current.orientation

    private let spokenInstructionObserver: T

    let styleURL: URL
    @Binding var camera: MapViewCamera
    let navigationCamera: MapViewCamera

    private var navigationState: NavigationState?
    private let userLayers: [StyleLayerDefinition]

    public var topCenter: (() -> AnyView)?
    public var topTrailing: (() -> AnyView)?
    public var midLeading: (() -> AnyView)?
    public var bottomTrailing: (() -> AnyView)?

    var onTapExit: (() -> Void)?
    let showMute: Bool

    public var minimumSafeAreaInsets: EdgeInsets

    /// Create a dynamically orienting navigation view. This view automatically arranges child views for both portrait
    /// and landscape orientations.
    ///
    /// - Parameters:
    ///   - styleURL: The map's style url.
    ///   - camera: The camera binding that represents the current camera on the map.
    ///   - navigationCamera: The default navigation camera. This sets the initial camera & is also used when the center
    /// on user button it tapped.
    ///   - navigationState: The current ferrostar navigation state provided by the Ferrostar core.
    ///   - minimumSafeAreaInsets: The minimum padding to apply from safe edges. See `complementSafeAreaInsets`.
    ///   - onTapExit: An optional behavior to run when the ArrivalView exit button is tapped. When nil (default) the
    /// exit button is hidden.
    ///   - makeMapContent: Custom maplibre symbols to display on the map view.
    public init(
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        navigationCamera: MapViewCamera = .automotiveNavigation(),
        navigationState: NavigationState?,
        minimumSafeAreaInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
        showMute: Bool = false,
        spokenInstructionObserver: T = DummyInstructionObserver(),
        onTapExit: (() -> Void)? = nil,
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        self.minimumSafeAreaInsets = minimumSafeAreaInsets
        self.showMute = showMute
        self.spokenInstructionObserver = spokenInstructionObserver
        self.onTapExit = onTapExit

        userLayers = makeMapContent()

        _camera = camera
        self.navigationCamera = navigationCamera
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
                .navigationMapViewContentInset(NavigationMapViewContentInsetMode(
                    orientation: orientation,
                    geometry: geometry
                ))

                switch orientation {
                case .landscapeLeft, .landscapeRight:
                    LandscapeNavigationOverlayView(
                        navigationState: navigationState,
                        speedLimit: nil,
                        showZoom: true,
                        onZoomIn: { camera.incrementZoom(by: 1) },
                        onZoomOut: { camera.incrementZoom(by: -1) },
                        showCentering: !camera.isTrackingUserLocationWithCourse,
                        onCenter: { camera = navigationCamera },
                        showMute: showMute,
                        spokenInstructionObserver: spokenInstructionObserver,
                        onTapExit: onTapExit
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
                default:
                    PortraitNavigationOverlayView(
                        navigationState: navigationState,
                        speedLimit: nil,
                        showZoom: true,
                        onZoomIn: { camera.incrementZoom(by: 1) },
                        onZoomOut: { camera.incrementZoom(by: -1) },
                        showCentering: !camera.isTrackingUserLocationWithCourse,
                        onCenter: { camera = navigationCamera },
                        showMute: showMute,
                        spokenInstructionObserver: spokenInstructionObserver,
                        onTapExit: onTapExit
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
        .onReceive(
            NotificationCenter.default.publisher(for: UIDevice.orientationDidChangeNotification)
        ) { _ in
            orientation = UIDevice.current.orientation
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

    return DynamicallyOrientingNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
        navigationState: state
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

    return DynamicallyOrientingNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        camera: .constant(.center(userLocation.clLocation.coordinate, zoom: 12)),
        navigationState: state
    )
    .navigationFormatterCollection(FoundationFormatterCollection(distanceFormatter: formatter))
}
