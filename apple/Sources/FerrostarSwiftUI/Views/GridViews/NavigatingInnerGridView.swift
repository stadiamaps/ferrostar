import FerrostarCore
import SwiftUI

/// When navigation is underway, we use this standardized grid view with pre-defined metadata and interactions.
/// This is the default UI and can be customized to some extent. If you need more customization,
/// use the ``InnerGridView``.
public struct NavigatingInnerGridView: View, CustomizableNavigatingInnerGridView {
    @Environment(\.navigationFormatterCollection) var formatterCollection: any FormatterCollection

    var speedLimit: Measurement<UnitSpeed>?
    var speedLimitStyle: SpeedLimitView.SignageStyle?

    let showZoom: Bool
    let onZoomIn: () -> Void
    let onZoomOut: () -> Void

    let showCentering: Bool
    let onCenter: () -> Void

    let showMute: Bool
    let isMuted: Bool
    let onMute: () -> Void

    // MARK: Customizable Containers

    public var topCenter: (() -> AnyView)?
    public var topTrailing: (() -> AnyView)?
    public var midLeading: (() -> AnyView)?
    public var bottomTrailing: (() -> AnyView)?

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
    ///   - showCentering: Whether to show the centering control. This is typically determined by the Map's centering
    /// state.
    ///   - onCenter: The action that occurs when the user taps the centering control (to re-center the map on the
    /// user).
    ///   - showMute: Whether to show the provided mute toggle or not.
    ///   - spokenInstructionObserver: The spoken instruction observer (for driving mute button state).
    public init(
        speedLimit: Measurement<UnitSpeed>? = nil,
        speedLimitStyle: SpeedLimitView.SignageStyle? = nil,
        isMuted: Bool,
        showMute: Bool = true,
        onMute: @escaping () -> Void,
        showZoom: Bool = false,
        onZoomIn: @escaping () -> Void = {},
        onZoomOut: @escaping () -> Void = {},
        showCentering: Bool = false,
        onCenter: @escaping () -> Void = {}
    ) {
        self.speedLimit = speedLimit
        self.speedLimitStyle = speedLimitStyle
        self.isMuted = isMuted
        self.showMute = showMute
        self.onMute = onMute
        self.showZoom = showZoom
        self.onZoomIn = onZoomIn
        self.onZoomOut = onZoomOut
        self.showCentering = showCentering
        self.onCenter = onCenter
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
            topCenter: { topCenter?() },
            topTrailing: {
                if showMute {
                    MuteUIButton(isMuted: isMuted, action: onMute)
                        .shadow(radius: 8)
                }
            },
            midLeading: { midLeading?() },
            midCenter: {
                // This view does not allow center content.
                Spacer()
            },
            midTrailing: {
                if showZoom {
                    NavigationUIZoomButton(onZoomIn: onZoomIn, onZoomOut: onZoomOut)
                        .shadow(radius: 8)
                } else {
                    Spacer()
                }
            },
            bottomLeading: {
                if showCentering {
                    NavigationUIButton(action: onCenter) {
                        Image(systemName: "location.north.fill")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 18, height: 18)
                    }
                    .shadow(radius: 8)
                } else {
                    Spacer()
                }
            },
            bottomCenter: {
                // This view does not allow center content to prevent overlaying the puck.
                Spacer()
            },
            bottomTrailing: { bottomTrailing?() }
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
