import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public struct CarPlayNavigationView: View {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection
    @Environment(\.navigationInnerGridConfiguration) private var gridConfig
    @Environment(\.navigationViewComponentsConfiguration) private var componentsConfig
    @Environment(\.navigationMapViewContentInsetConfiguration) private var mapInsetConfig

    let styleURL: URL
    @Binding var camera: MapViewCamera
    let navigationCamera: MapViewCamera

    private let navigationState: NavigationState?
    private let userLayers: [StyleLayerDefinition]

    public var minimumSafeAreaInsets: EdgeInsets

    /// Create a landscape navigation view. This view is optimized for display on a landscape screen where the
    /// instructions are on the leading half of the screen
    /// and the user puck and route are on the trailing half of the screen.
    ///
    /// - Parameters:
    ///   - styleURL: The map's style url.
    ///   - camera: The camera binding that represents the current camera on the map.
    ///   - navigationCamera: The default navigation camera. This sets the initial camera & is also used when the center
    /// on user button it tapped.
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
        minimumSafeAreaInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        self.minimumSafeAreaInsets = minimumSafeAreaInsets

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
                .navigationMapViewContentInset(
                    calculatedMapViewInsets(for: geometry)
                )

                CarPlayNavigationOverlayView(
                    navigationState: navigationState,
                    cameraControlState: camera.isTrackingUserLocationWithCourse ? CameraControlState.showRecenter {
                        // Does nothing on CarPlay
                    } : .showRecenter {
                        // Does nothing on CarPlay
                    }
                )
                .navigationViewInnerGrid {
                    gridConfig.getTopCenter()
                } topTrailing: {
                    gridConfig.getTopTrailing()
                } midLeading: {
                    gridConfig.getMidLeading()
                } bottomLeading: {
                    gridConfig.getBottomLeading()
                } bottomTrailing: {
                    gridConfig.getBottomTrailing()
                }
            }
        }
    }

    func calculatedMapViewInsets(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        if case .rect = camera.state {
            .edgeInset(UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 0))
        } else {
            // Use convenience accessor that handles fallback automatically
            mapInsetConfig.getLandscapeInset(for: geometry)
        }
    }
}
