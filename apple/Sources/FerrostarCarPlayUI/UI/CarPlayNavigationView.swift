import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public struct CarPlayNavigationView: View {
    @Environment(\.navigationViewComponentsConfiguration) private var componentsConfig
    @Environment(\.navigationMapViewContentInsetConfiguration) private var mapInsetConfig

    private let navigationState: NavigationState?

    let styleURL: URL

    @Binding public var camera: MapViewCamera

    private let userLayers: [StyleLayerDefinition]

    public var speedLimit: Measurement<UnitSpeed>?
    public var speedLimitStyle: SpeedLimitView.SignageStyle?

    public init(
        navigationState: NavigationState?,
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.navigationState = navigationState
        self.styleURL = styleURL
        _camera = camera
        userLayers = makeMapContent()
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                NavigationMapView(
                    styleURL: styleURL,
                    camera: $camera,
                    navigationState: navigationState,
                    activity: .carplay,
                    onStyleLoaded: { _ in
                        // camera = .automotiveNavigation(zoom: 17.0)
                    }
                ) {
                    userLayers
                }
                .navigationMapViewContentInset(
                    mapInsetConfig.getLandscapeInset(for: geometry)
                )
            }
        }
    }
}
