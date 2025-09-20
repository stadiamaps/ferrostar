import FerrostarCore
import SwiftUI

/// Determines which camera control is shown in the navigation inner grid view.
public enum CameraControlState {
    /// Don't show any camera control.
    case hidden

    /// Shows the recenter button.
    ///
    /// The action is responsible for resetting the camera
    /// to a state that follows the user.
    case showRecenter(() -> Void)

    /// Shows the route overview button.
    ///
    /// The action is responsible for transitioning the camera to an overview of the route.
    case showRouteOverview(() -> Void)
}

/// When navigation is underway, we use this standardized grid view with pre-defined metadata and interactions.
/// This is the default UI and can be customized to some extent. If you need more customization,
/// use the ``InnerGridView``.
public struct NavigatingInnerGridView: View {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection
    @Environment(\.navigationInnerGridConfiguration) private var gridConfig

    var speedLimit: Measurement<UnitSpeed>?
    var speedLimitStyle: SpeedLimitView.SignageStyle?

    let showZoom: Bool
    let onZoomIn: () -> Void
    let onZoomOut: () -> Void

    let cameraControlState: CameraControlState

    let showMute: Bool
    let isMuted: Bool
    let onMute: () -> Void

    /// The default navigation inner grid view.
    ///
    /// This view provides all default navigation UI views that are used in the open map area. This area is defined as
    /// between the header/banner view and footer/trip progress view in portrait mode.
    /// On landscape mode it is the trailing half of the screen.
    ///
    /// - Parameters:
    ///   - speedLimit: The speed limit provided by the navigation state (or nil)
    ///   - speedLimitStyle: The speed limit style: Vienna Convention (most of the world) or MUTCD (US primarily).
    ///   - isMuted: Is speech currently muted?
    ///   - showMute: Whether to show the provided mute button or not.
    ///   - showZoom: Whether to show the provided zoom control or not.
    ///   - onZoomIn: The on zoom in tapped action. This should be used to zoom the user in one increment.
    ///   - onZoomOut: The on zoom out tapped action. This should be used to zoom the user out one increment.
    ///   - cameraControlState: Which camera control to show (and its respective action).
    public init(
        speedLimit: Measurement<UnitSpeed>? = nil,
        speedLimitStyle: SpeedLimitView.SignageStyle? = nil,
        isMuted: Bool,
        showMute: Bool = true,
        onMute: @escaping () -> Void,
        showZoom: Bool = false,
        onZoomIn: @escaping () -> Void = {},
        onZoomOut: @escaping () -> Void = {},
        cameraControlState: CameraControlState = .hidden
    ) {
        self.speedLimit = speedLimit
        self.speedLimitStyle = speedLimitStyle
        self.isMuted = isMuted
        self.showMute = showMute
        self.onMute = onMute
        self.showZoom = showZoom
        self.onZoomIn = onZoomIn
        self.onZoomOut = onZoomOut
        self.cameraControlState = cameraControlState
    }

    public var body: some View {
        InnerGridView(
            topLeading: {
                if let speedLimit, let speedLimitStyle {
                    SpeedLimitView(
                        speedLimit: speedLimit,
                        signageStyle: speedLimitStyle,
                        valueFormatter: formatterCollection.speedValueFormatter,
                        unitFormatter: formatterCollection.speedWithUnitsFormatter
                    )
                }
            },
            topCenter: { gridConfig.getTopCenter() },
            topTrailing: {
                // TODO: Extract to a separate view?
                switch cameraControlState {
                case .hidden:
                    // Nothing
                    EmptyView()
                case let .showRecenter(onCenter):
                    NavigationUIButton(action: onCenter) {
                        Image(systemName: "location.north.fill")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 18, height: 18)
                    }
                    .shadow(radius: 8)
                case let .showRouteOverview(onRouteOverview):
                    NavigationUIButton(action: onRouteOverview) {
                        Image(systemName: "point.bottomleft.forward.to.point.topright.scurvepath")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 18, height: 18)
                    }
                    .shadow(radius: 8)
                }

                if showMute {
                    NavigationUIMuteButton(isMuted: isMuted, action: onMute)
                }
            },
            midLeading: { gridConfig.getMidLeading() },
            midCenter: {
                // This view does not allow center content.
                Spacer()
            },
            midTrailing: {
                if showZoom {
                    NavigationUIZoomButton(onZoomIn: onZoomIn, onZoomOut: onZoomOut)
                } else {
                    Spacer()
                }
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

#Preview("Navigating Inner Minimal Example") {
    VStack(spacing: 16) {
        RoundedRectangle(cornerRadius: 12)
            .padding(.horizontal, 16)
            .frame(height: 128)

        NavigatingInnerGridView(
            speedLimit: .init(value: 55, unit: .milesPerHour),
            speedLimitStyle: .viennaConvention,
            isMuted: true,
            showMute: true,
            onMute: {}
        )
        .padding(.horizontal, 16)

        RoundedRectangle(cornerRadius: 36)
            .padding(.horizontal, 16)
            .frame(height: 72)
    }
    .background(Color.green)
}
