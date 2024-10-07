import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

struct PortraitNavigationOverlayView: View, CustomizableNavigatingInnerGridView {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    private var navigationState: NavigationState?

    @State private var isInstructionViewExpanded: Bool = false
    @State private var instructionsViewSizeWhenNotExpanded: CGSize = .zero

    var topCenter: (() -> AnyView)?
    var topTrailing: (() -> AnyView)?
    var midLeading: (() -> AnyView)?
    var bottomTrailing: (() -> AnyView)?

    var speedLimit: Measurement<UnitSpeed>?
    var showZoom: Bool
    var onZoomIn: () -> Void
    var onZoomOut: () -> Void
    var showCentering: Bool
    var onCenter: () -> Void
    var onTapExit: (() -> Void)?

    init(
        navigationState: NavigationState?,
        speedLimit: Measurement<UnitSpeed>? = nil,
        showZoom: Bool = false,
        onZoomIn: @escaping () -> Void = {},
        onZoomOut: @escaping () -> Void = {},
        showCentering: Bool = false,
        onCenter: @escaping () -> Void = {},
        onTapExit: (() -> Void)? = nil
    ) {
        self.navigationState = navigationState
        self.speedLimit = speedLimit
        self.showZoom = showZoom
        self.onZoomIn = onZoomIn
        self.onZoomOut = onZoomOut
        self.showCentering = showCentering
        self.onCenter = onCenter
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
                    showZoom: showZoom,
                    onZoomIn: onZoomIn,
                    onZoomOut: onZoomOut,
                    showCentering: showCentering,
                    onCenter: onCenter
                )
                .innerGrid {
                    topCenter?()
                } topTrailing: {
                    topTrailing?()
                } midLeading: {
                    midLeading?()
                } bottomTrailing: {
                    bottomTrailing?()
                }

                if case .navigating = navigationState?.tripState,
                   let progress = navigationState?.currentProgress
                {
                    ArrivalView(
                        progress: progress,
                        onTapExit: onTapExit
                    )
                }
            }
            .padding(.top, instructionsViewSizeWhenNotExpanded.height + 16)

            if case .navigating = navigationState?.tripState,
               let visualInstruction = navigationState?.currentVisualInstruction,
               let progress = navigationState?.currentProgress,
               let remainingSteps = navigationState?.remainingSteps
            {
                InstructionsView(
                    visualInstruction: visualInstruction,
                    distanceFormatter: formatterCollection.distanceFormatter,
                    distanceToNextManeuver: progress.distanceToNextManeuver,
                    remainingSteps: remainingSteps,
                    isExpanded: $isInstructionViewExpanded,
                    sizeWhenNotExpanded: $instructionsViewSizeWhenNotExpanded
                )
            }
        }
    }
}
