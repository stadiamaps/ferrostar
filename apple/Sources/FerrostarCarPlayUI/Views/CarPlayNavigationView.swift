import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public struct CarPlayNavigationView: View, SpeedLimitViewHost, CurrentRoadNameViewHost {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let styleURL: URL
    @Binding var camera: MapViewCamera
    let navigationCamera: MapViewCamera
    private let userLayers: (NavigationState?) -> [StyleLayerDefinition]

    private let navigationState: NavigationState?

    public var speedLimit: Measurement<UnitSpeed>?
    public var speedLimitStyle: SpeedLimitView.SignageStyle?

    public var currentRoadNameView: ((NavigationState?) -> AnyView)?

    public var minimumSafeAreaInsets: EdgeInsets

    // MARK: Configurable Views

    public init(
        styleURL: URL,
        camera: Binding<MapViewCamera>,
        navigationCamera: MapViewCamera = .automotiveNavigation(),
        navigationState: NavigationState?,
        minimumSafeAreaInsets: EdgeInsets = EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
        @MapViewContentBuilder mapContent: @escaping (NavigationState?) -> some StyleLayerCollection = { _ in [] }
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        self.minimumSafeAreaInsets = minimumSafeAreaInsets

        _camera = camera
        self.navigationCamera = navigationCamera
        userLayers = { state in mapContent(state).layers }
        currentRoadNameView = { AnyView(CurrentRoadNameView(currentRoadName: $0?.currentRoadName)) }
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
                        camera = navigationCamera
                    }
                ) {
                    userLayers(navigationState)
                }
                .navigationMapViewContentInset(calculatedMapViewInsets(for: geometry))

                if let speedLimit, let speedLimitStyle {
                    SpeedLimitView(speedLimit: speedLimit, signageStyle: speedLimitStyle)
                        .scaleEffect(scaleFactor(for: geometry))
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                        .padding(8)
                }

                if let currentRoadNameView {
                    HStack {
                        Spacer()

                        currentRoadNameView(navigationState)
                            .scaleEffect(scaleFactor(for: geometry))
                            .padding(.trailing, geometry.safeAreaInsets.trailing)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                    }
                }
            }
        }
    }

    func calculatedMapViewInsets(for geometry: GeometryProxy) -> NavigationMapViewContentInsetMode {
        let safeArea = geometry.safeAreaInsets
        return if case .rect = camera.state {
            .edgeInset(UIEdgeInsets(
                top: safeArea.top,
                left: geometry.size.width / 2,
                bottom: safeArea.bottom,
                right: 32
            ))
        } else {
            .edgeInset(UIEdgeInsets(
                top: geometry.size.width * 0.80,
                left: geometry.size.width * 0.80,
                bottom: 16,
                right: 16
            ))
        }
    }

    func scaleFactor(for geometry: GeometryProxy) -> CGFloat {
        switch geometry.size.width {
        case 0 ..< 375:
            0.6
        default:
            1.0
        }
    }
}
