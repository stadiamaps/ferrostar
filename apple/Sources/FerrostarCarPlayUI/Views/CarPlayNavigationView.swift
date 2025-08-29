import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

public struct CarPlayNavigationView: View, SpeedLimitViewHost {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let styleURL: URL
    @Binding var camera: MapViewCamera
    let navigationCamera: MapViewCamera
    private let userLayers: (NavigationState?) -> [StyleLayerDefinition]

    private let navigationState: NavigationState?

    public var speedLimit: Measurement<UnitSpeed>?
    public var speedLimitStyle: SpeedLimitView.SignageStyle?

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
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                if let speedLimit, let speedLimitStyle {
                    VStack {
                        HStack {
                            Spacer()

                            SpeedLimitView(speedLimit: speedLimit, signageStyle: speedLimitStyle)
                                .padding()
                        }

                        Spacer()
                    }
                }

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
}
