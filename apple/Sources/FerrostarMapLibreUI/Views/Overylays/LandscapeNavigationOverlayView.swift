import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

struct LandscapeNavigationOverlayView: View, CustomizableNavigatingInnerGridView {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

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

    // MARK: Configurable Views

    var topCenter: (() -> AnyView)?
    var topTrailing: (() -> AnyView)?
    var midLeading: (() -> AnyView)?
    var bottomLeading: (() -> AnyView)?
    var bottomTrailing: (() -> AnyView)?

    var progressView: (NavigationState?, (() -> Void)?) -> AnyView
    var instructionsView: (NavigationState?, Binding<Bool>, Binding<CGSize>) -> AnyView
    var currentRoadNameView: (NavigationState?) -> AnyView

    // NOTE: These don't really follow our usual coding style as they are internal.
    init(
        navigationState: NavigationState?,
        speedLimit: Measurement<UnitSpeed>? = nil,
        speedLimitStyle: SpeedLimitView.SignageStyle? = nil,
        views: NavigationViewComponentBuilder,
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

        progressView = views.progressView
        instructionsView = views.instructionsView
        currentRoadNameView = views.currentRoadNameView
    }

    var body: some View {
        HStack {
            ZStack(alignment: .top) {
                VStack {
                    Spacer()

                    HStack {
                        progressView(navigationState, onTapExit)
                    }
                }

                instructionsView(navigationState, $isInstructionViewExpanded, .constant(.zero))
            }

            Spacer().frame(width: 16)

            ZStack(alignment: .bottom) {
                // Centering will push up the grid. Allowing for the road name
                if case .hidden = cameraControlState {
                    HStack {
                        Spacer(minLength: 64)

                        currentRoadNameView(navigationState)

                        Spacer(minLength: 64)
                    }
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
                .innerGrid {
                    topCenter?()
                } topTrailing: {
                    topTrailing?()
                } midLeading: {
                    midLeading?()
                } bottomLeading: {
                    bottomLeading?()
                } bottomTrailing: {
                    bottomTrailing?()
                }
            }
        }
    }
}
