import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A portrait orientation navigation view that includes the InstructionsView at the top.
public struct PortraitNavigationView<TopCenter: View, TopTrailing: View, MidLeading: View, BottomTrailing: View>: View {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    let styleURL: URL
    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?
    private let userLayers: [StyleLayerDefinition]

    var topCenter: TopCenter
    var topTrailing: TopTrailing
    var midLeading: MidLeading
    var bottomTrailing: BottomTrailing

    @Binding var camera: MapViewCamera
    @Binding var snappedZoom: Double
    @Binding var useSnappedCamera: Bool

    var onTapExit: (() -> Void)?

    public init(
        styleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>,
        snappedZoom: Binding<Double>,
        useSnappedCamera: Binding<Bool>,
        onTapExit: (() -> Void)? = nil,
        @MapViewContentBuilder makeMapContent: () -> [StyleLayerDefinition] = { [] },
        @ViewBuilder topCenter: () -> TopCenter = { Spacer() },
        @ViewBuilder topTrailing: () -> TopTrailing = { Spacer() },
        @ViewBuilder midLeading: () -> MidLeading = { Spacer() },
        @ViewBuilder bottomTrailing: () -> BottomTrailing = { Spacer() }
    ) {
        self.styleURL = styleURL
        self.navigationState = navigationState
        self.onTapExit = onTapExit

        userLayers = makeMapContent()
        self.topCenter = topCenter()
        self.topTrailing = topTrailing()
        self.midLeading = midLeading()
        self.bottomTrailing = bottomTrailing()

        _camera = camera
        _snappedZoom = snappedZoom
        _useSnappedCamera = useSnappedCamera
    }

    public var body: some View {
        GeometryReader { geometry in
            ZStack {
                NavigationMapView(
                    styleURL: styleURL,
                    navigationState: navigationState,
                    camera: $camera,
                    snappedZoom: $snappedZoom,
                    useSnappedCamera: $useSnappedCamera
                ) {
                    userLayers
                }
                .navigationMapViewContentInset(.portrait(within: geometry))

                VStack {
                    if let navigationState,
                       let visualInstructions = navigationState.visualInstruction
                    {
                        InstructionsView(
                            visualInstruction: visualInstructions,
                            distanceFormatter: formatterCollection.distanceFormatter,
                            distanceToNextManeuver: navigationState.progress?.distanceToNextManeuver
                        )
                    }

                    // The inner content is displayed vertically full screen
                    // when both the visualInstructions and progress are nil.
                    // It will automatically reduce height if and when either
                    // view appears
                    // TODO: Add dynamic speed, zoom & centering.
                    NavigatingInnerGridView(
                        speedLimit: nil,
                        showZoom: false,
                        onZoomIn: {},
                        onZoomOut: {},
                        showCentering: false,
                        onCenter: {},
                        topCenter: { topCenter },
                        topTrailing: { topTrailing },
                        midLeading: { midLeading },
                        bottomTrailing: { bottomTrailing }
                    )
                    .padding(.horizontal, 16)

                    if let progress = navigationState?.progress {
                        ArrivalView(
                            progress: progress,
                            onTapExit: onTapExit
                        )
                        .padding(.horizontal, 16)
                    }
                }
            }
        }
    }
}

#Preview("Portrait Navigation View (Imperial)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    return PortraitNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true)
    )
    .navigationFormatterCollection(FoundationFormatterCollection(distanceFormatter: formatter))
}

#Preview("Portrait Navigation View (Metric)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .metric

    return PortraitNavigationView(
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true)
    )
    .navigationFormatterCollection(FoundationFormatterCollection(distanceFormatter: formatter))
}
