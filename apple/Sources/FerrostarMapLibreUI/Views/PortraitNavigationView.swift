import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

/// A portrait orientation navigation view that includes the InstructionsView at the top.
public struct PortraitNavigationView<TopCenter: View, TopTrailing: View, MidLeading: View, BottomTrailing: View>: View {
    let theme: any FerrostarTheme

    let styleURL: URL
    // TODO: Configurable camera and user "puck" rotation modes

    private var navigationState: NavigationState?
    private let userLayers: [StyleLayerDefinition]

    @ViewBuilder var topCenter: () -> TopCenter
    @ViewBuilder var topTrailing: () -> TopTrailing
    @ViewBuilder var midLeading: () -> MidLeading
    @ViewBuilder var bottomTrailing: () -> BottomTrailing

    @Binding var camera: MapViewCamera
    @Binding var snappedZoom: Double
    @Binding var useSnappedCamera: Bool

    var onTapExit: () -> Void

    public init(
        theme: any FerrostarTheme = DefaultFerrostarTheme(),
        styleURL: URL,
        navigationState: NavigationState?,
        camera: Binding<MapViewCamera>,
        snappedZoom: Binding<Double>,
        useSnappedCamera: Binding<Bool>,
        onTapExit: @escaping () -> Void = {},
        @MapViewContentBuilder mapContent: () -> [StyleLayerDefinition] = { [] },
        @ViewBuilder topCenter: @escaping () -> TopCenter = { InfiniteSpacer() },
        @ViewBuilder topTrailing: @escaping () -> TopTrailing = { InfiniteSpacer() },
        @ViewBuilder midLeading: @escaping () -> MidLeading = { InfiniteSpacer() },
        @ViewBuilder bottomTrailing: @escaping () -> BottomTrailing = { InfiniteSpacer() }
    ) {
        self.theme = theme
        self.styleURL = styleURL
        self.navigationState = navigationState
        userLayers = mapContent()
        self.onTapExit = onTapExit

        self.topCenter = topCenter
        self.topTrailing = topTrailing
        self.midLeading = midLeading
        self.bottomTrailing = bottomTrailing

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
                            distanceFormatter: theme.distanceFormatter,
                            distanceToNextManeuver: navigationState.progress?.distanceToNextManeuver
                        )
                    }

                    // The inner content is displayed vertically full screen
                    // when both the visualInstructions and progress are nil.
                    // It will automatically reduce height if and when either
                    // view appears
                    // TODO: Add dynamic speed, zoom & centering.
                    NavigatingInnerGridView(
                        theme: theme,
                        speedLimit: nil,
                        showZoom: false,
                        onZoomIn: {},
                        onZoomOut: {},
                        showCentering: false,
                        onCenter: {},
                        topCenter: topCenter,
                        topTrailing: topTrailing,
                        midLeading: midLeading,
                        bottomTrailing: bottomTrailing
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

    let theme = DefaultFerrostarTheme(distanceFormatter: formatter)

    return PortraitNavigationView(
        theme: theme,
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true)
    )
}

#Preview("Portrait Navigation View (Metric)") {
    // TODO: Make map URL configurable but gitignored
    let state = NavigationState.modifiedPedestrianExample(droppingNWaypoints: 4)

    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .metric

    let theme = DefaultFerrostarTheme(distanceFormatter: formatter)

    return PortraitNavigationView(
        theme: theme,
        styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
        navigationState: state,
        camera: .constant(.center(state.snappedLocation.clLocation.coordinate, zoom: 12)),
        snappedZoom: .constant(18),
        useSnappedCamera: .constant(true)
    )
}
