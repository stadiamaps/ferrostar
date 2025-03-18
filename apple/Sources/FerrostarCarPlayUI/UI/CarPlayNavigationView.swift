import FerrostarCore
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public struct CarPlayNavigationView: View, SpeedLimitViewHost,
    CurrentRoadNameViewHost
{
    @ObservedObject var ferrostarCore: FerrostarCore
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let styleURL: URL

    @State var camera: MapViewCamera
    public var currentRoadNameView: AnyView?

    private let userLayers: [StyleLayerDefinition]

    public var speedLimit: Measurement<UnitSpeed>?
    public var speedLimitStyle: SpeedLimitView.SignageStyle?

    public var minimumSafeAreaInsets: EdgeInsets

    public init(
        ferrostarCore: FerrostarCore,
        styleURL: URL,
        camera: MapViewCamera = .automotiveNavigation(zoom: 17.0),
        minimumSafeAreaInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.ferrostarCore = ferrostarCore
        self.styleURL = styleURL
        self.camera = camera
        self.minimumSafeAreaInsets = minimumSafeAreaInsets
        userLayers = makeMapContent()
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                NavigationMapView(
                    styleURL: styleURL,
                    camera: $camera,
                    navigationState: ferrostarCore.state
                ) { _ in
                }
                .navigationMapViewContentInset(.landscape(within: geometry, horizontalPct: 0.65))
            }
        }
    }
}
