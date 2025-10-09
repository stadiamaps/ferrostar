import FerrostarCore
import FerrostarSwiftUI
import MapKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

struct CarPlayNavigationOverlayView: View {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection
    @Environment(\.navigationInnerGridConfiguration) private var gridConfig
    @Environment(\.navigationViewComponentsConfiguration) private var componentsConfig
    @Environment(\.speedLimitConfiguration) private var speedLimitConfig

    private let navigationState: NavigationState?
    private let cameraControlState: CameraControlState

    // NOTE: These don't really follow our usual coding style as they are internal.
    init(
        navigationState: NavigationState?,
        cameraControlState: CameraControlState
    ) {
        self.navigationState = navigationState
        self.cameraControlState = cameraControlState
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            // Centering will push up the grid. Allowing for the road name
            switch cameraControlState {
            case .hidden, .showRouteOverview:
                HStack {
                    Spacer()

                    componentsConfig.getCurrentRoadNameView(navigationState)
                        .scaleEffect(0.5)

                    Spacer()
                }
            case .showRecenter:
                EmptyView()
            }

            InnerGridView(
                topLeading: {
                    if let speedLimit = speedLimitConfig.speedLimit {
                        SpeedLimitView(
                            speedLimit: speedLimit,
                            signageStyle: speedLimitConfig.speedLimitStyle,
                            valueFormatter: formatterCollection.speedValueFormatter,
                            unitFormatter: formatterCollection.speedWithUnitsFormatter
                        )
                        .scaleEffect(0.6)
                    }
                },
                topCenter: { gridConfig.getTopCenter() },
                topTrailing: {
                    Spacer()
                },
                midLeading: { gridConfig.getMidLeading() },
                midCenter: {
                    // This view does not allow center content.
                    Spacer()
                },
                midTrailing: {
                    Spacer()
                },
                bottomLeading: { gridConfig.getBottomLeading() },
                bottomCenter: {
                    // This view does not allow center content to prevent overlaying the puck.
                    Spacer()
                },
                bottomTrailing: { gridConfig.getBottomTrailing() }
            )
        }
    }
}
