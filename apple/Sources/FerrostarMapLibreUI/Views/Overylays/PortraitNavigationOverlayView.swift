import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

struct PortraitNavigationOverlayView: View {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection
    @Environment(\.navigationInnerGridConfiguration) private var gridConfig
    @Environment(\.navigationViewComponentsConfiguration) private var componentsConfig

    private let navigationState: NavigationState?

    @State private var isInstructionViewExpanded: Bool = false
    @State private var instructionsViewSizeWhenNotExpanded: CGSize = .zero

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
        self.showMute = showMute
        self.showZoom = showZoom
        self.onMute = onMute
        self.onZoomIn = onZoomIn
        self.onZoomOut = onZoomOut
        self.cameraControlState = cameraControlState
        self.onTapExit = onTapExit
    }

    var body: some View {
        ZStack(alignment: .top) {
            VStack {
                Spacer()

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

                if case .navigating = navigationState?.tripState {
                    VStack {
                        switch cameraControlState {
                        case .hidden, .showRouteOverview:
                            componentsConfig.getCurrentRoadNameView(navigationState)
                        case .showRecenter:
                            EmptyView()
                        }

                        componentsConfig.getProgressView(navigationState, onTapExit: onTapExit)
                    }
                }
            }
            .padding(.top, topPadding + 16)

            if case .offRoute = navigationState?.currentDeviation {
                componentsConfig.getOffRouteView(navigationState)
            } else {
                componentsConfig.getInstructionsView(
                    navigationState,
                    isExpanded: $isInstructionViewExpanded,
                    sizeWhenNotExpanded: $instructionsViewSizeWhenNotExpanded
                )
            }
        }
    }

    private var topPadding: CGFloat {
        guard case .navigating = navigationState?.tripState else {
            return 0
        }

        if case .offRoute = navigationState?.currentDeviation {
            return 48
        }

        return instructionsViewSizeWhenNotExpanded.height
    }
}
