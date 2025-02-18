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
    @EnvironmentObject var ferrostarCore: FerrostarCore

    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let styleURL: URL

    @State var camera: MapViewCamera
    public var currentRoadNameView: AnyView?

    private let userLayers: [StyleLayerDefinition]

    public var speedLimit: Measurement<UnitSpeed>?
    public var speedLimitStyle: SpeedLimitView.SignageStyle?

    public var minimumSafeAreaInsets: EdgeInsets

    public init(
        styleURL: URL,
        camera: MapViewCamera = .automotiveNavigation(),
        minimumSafeAreaInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
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
                ) {
                    userLayers
                }
                .navigationMapViewContentInset(.landscape(within: geometry))
            }
        }
    }
}
