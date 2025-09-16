import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

struct LandscapeNavigationOverlayView: View {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection
    @Environment(\.navigationInnerGridConfiguration) private var gridConfig
    @Environment(\.navigationViewComponentsConfiguration) private var componentsConfig

    private let navigationState: NavigationState?

    @State private var isInstructionViewExpanded: Bool = false

    var speedLimit: Measurement<UnitSpeed>?
    var speedLimitStyle: SpeedLimitView.SignageStyle?

    var showZoom: Bool
    var onZoomIn: () -> Void
    var onZoomOut: () -> Void

    var cameraControlState: CameraControlState

    var onTapExit: (() -> Void)?

    let showMute: Bool
    let isMuted: Bool
    let onMute: () -> Void

    // NOTE: These don't really follow our usual coding style as they are internal.
    init(
        navigationState: NavigationState?,
        speedLimit: Measurement<UnitSpeed>? = nil,
        speedLimitStyle: SpeedLimitView.SignageStyle? = nil,
        isMuted: Bool,
        showMute: Bool = true,
        onMute: @escaping () -> Void,
        showZoom: Bool = false,
        onZoomIn: @escaping () -> Void = {},
        onZoomOut: @escaping () -> Void = {},
        cameraControlState: CameraControlState = .hidden,
        onTapExit: (() -> Void)? = nil
    ) {
        self.navigationState = navigationState
        self.speedLimit = speedLimit
        self.speedLimitStyle = speedLimitStyle
        self.isMuted = isMuted
        self.onMute = onMute
        self.showMute = showMute
        self.showZoom = showZoom
        self.onZoomIn = onZoomIn
        self.onZoomOut = onZoomOut
        self.cameraControlState = cameraControlState
        self.onTapExit = onTapExit
    }

    var body: some View {
        HStack {
            ZStack(alignment: .top) {
                VStack {
                    Spacer()

                    HStack {
                        componentsConfig.getProgressView(navigationState, onTapExit: onTapExit)
                    }
                }

                if case .offRoute = navigationState?.currentDeviation {
                    componentsConfig.getOffRouteView(
                        navigationState,
                        size: .constant(.zero)
                    )
                } else {
                    componentsConfig.getInstructionsView(
                        navigationState,
                        isExpanded: $isInstructionViewExpanded,
                        sizeWhenNotExpanded: .constant(.zero)
                    )
                }
            }

            Spacer().frame(width: 16)

            ZStack(alignment: .bottom) {
                // Centering will push up the grid. Allowing for the road name
                switch cameraControlState {
                case .hidden, .showRouteOverview:
                    HStack {
                        Spacer(minLength: 64)

                        componentsConfig.getCurrentRoadNameView(navigationState)

                        Spacer(minLength: 64)
                    }
                case .showRecenter:
                    EmptyView()
                }

                // The inner content is displayed vertically full screen
                // when both the visualInstructions and progress are nil.
                // It will automatically reduce height if and when either
                // view appears
                NavigatingInnerGridView(
                    speedLimit: speedLimit,
                    speedLimitStyle: speedLimitStyle,
                    isMuted: isMuted,
                    showMute: showMute,
                    onMute: onMute,
                    showZoom: showZoom,
                    onZoomIn: onZoomIn,
                    onZoomOut: onZoomOut,
                    cameraControlState: cameraControlState
                )
                .navigationViewInnerGrid {
                    gridConfig.getTopCenter()
                } topTrailing: {
                    gridConfig.getTopTrailing()
                } midLeading: {
                    gridConfig.getMidLeading()
                } bottomLeading: {
                    gridConfig.getBottomLeading()
                } bottomTrailing: {
                    gridConfig.getBottomTrailing()
                }
            }
        }
    }
}
