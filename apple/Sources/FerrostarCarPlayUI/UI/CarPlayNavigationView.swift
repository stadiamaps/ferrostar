import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public struct CarPlayNavigationView: View,
    SpeedLimitViewHost, NavigationViewConfigurable
{
    private let navigationState: NavigationState?

    let styleURL: URL

    @Binding public var camera: MapViewCamera
    public var mapInsets: NavigationMapViewContentInsetBundle

    private let userLayers: [StyleLayerDefinition]

    public var speedLimit: Measurement<UnitSpeed>?
    public var speedLimitStyle: SpeedLimitView.SignageStyle?

    public var progressView: ((NavigationState?, (() -> Void)?) -> AnyView)?
    public var instructionsView: ((NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView)?
    public var currentRoadNameView: ((NavigationState?) -> AnyView)?

    public init(
        navigationState: NavigationState?,
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.navigationState = navigationState
        self.styleURL = styleURL
        _camera = camera
        mapInsets = NavigationMapViewContentInsetBundle()
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
                .navigationMapViewContentInset(mapInsets.landscape(geometry))
            }
        }
    }
}
