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
    @EnvironmentObject var ferrostarCore: FerrostarCore
    
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let styleURL: URL
    @State var camera: MapViewCamera = .center(.init(latitude: 37.331726, longitude: -122.031790), zoom: 12)
    let navigationCamera: MapViewCamera
    public var currentRoadNameView: AnyView?

    private let userLayers: [StyleLayerDefinition]

    public var speedLimit: Measurement<UnitSpeed>?
    public var speedLimitStyle: SpeedLimitView.SignageStyle?

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
        navigationCamera: MapViewCamera = .automotiveNavigation(),
        minimumSafeAreaInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] }
    ) {
        self.styleURL = styleURL
        self.minimumSafeAreaInsets = minimumSafeAreaInsets

        userLayers = makeMapContent()
        self.navigationCamera = navigationCamera
        // TODO: Correct me
//        currentRoadNameView = AnyView(CurrentRoadNameView(currentRoadName: ferrostarCore.state?.currentRoadName))
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                NavigationMapView(
                    styleURL: styleURL,
                    camera: $camera,
                    navigationState: ferrostarCore.state,
                    onStyleLoaded: { _ in
                        camera = navigationCamera
                    }
                ) {
                    userLayers
                }
                .navigationMapViewContentInset(.landscape(within: geometry))
            }
//            .task {
//                try! await Task.sleep(for: .seconds(10))
//                camera = .center(.init(latitude: 37.131726, longitude: -122.031790), zoom: 10)
//            }
        }
    }
}

