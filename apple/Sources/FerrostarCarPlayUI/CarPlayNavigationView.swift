import SwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import FerrostarCore
import FerrostarSwiftUI
import FerrostarMapLibreUI

public struct CarPlayNavigationView: View, SpeedLimitViewHost,
    CurrentRoadNameViewHost
{
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let styleURL: URL = URL(string: "https://demotiles.maplibre.org/style.json")!
    @State var camera: MapViewCamera = .center(.init(latitude: 45.5, longitude: -122.6), zoom: 1)
    let navigationCamera: MapViewCamera = .automotiveNavigation()
    public var currentRoadNameView: AnyView?

    private var navigationState: NavigationState? = .pedestrianExample
    private let userLayers: [StyleLayerDefinition] = []

    public var speedLimit: Measurement<UnitSpeed>? = nil
    public var speedLimitStyle: SpeedLimitView.SignageStyle? = nil

    let isMuted: Bool = false

    public var minimumSafeAreaInsets: EdgeInsets = .init()

    public init() {
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
//                        camera = navigationCamera
                    }
                ) {
                    userLayers
                }
                .navigationMapViewContentInset(.landscape(within: geometry))
            }
        }
    }
}

