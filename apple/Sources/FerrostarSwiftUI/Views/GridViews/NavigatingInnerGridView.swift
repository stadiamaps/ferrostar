import SwiftUI

/// When navigation is underway, we use this standardized grid view with pre-defined metadata and interactions.
/// This is the default UI and can be customized to some extent, however more customization can be
/// achieved using the ``InnerGridView``.
public struct NavigatingInnerGridView<
    TopCenter: View,
    TopTrailing: View,
    MidLeading: View,
    BottomTrailing: View
>: View {
    var theme: any FerrostarTheme

    var speedLimit: Measurement<UnitSpeed>?

    var showZoom: Bool
    var onZoomIn: () -> Void
    var onZoomOut: () -> Void

    var showCentering: Bool
    var onCenter: () -> Void

    // MARK: Customizable Containers

    @ViewBuilder var topCenter: () -> TopCenter
    @ViewBuilder var topTrailing: () -> TopTrailing
    @ViewBuilder var midLeading: () -> MidLeading
    @ViewBuilder var bottomTrailing: () -> BottomTrailing

    /// The default navigation inner grid view.
    ///
    /// This view provides all default navigation UI views that are used in the open map area. This area is defined as
    /// between the header/banner view and footer/arrival view in portait mode.
    /// On landscape mode it is the trialing half of the screen.
    ///
    /// - Parameters:
    ///   - theme: The ferrostar theme is used to control the default styling and formatters
    ///   - speedLimit: The speed limit provided by the navigation state (or nil)
    ///   - showZoom: Whether to show the zoom control or not. This is typically yes.
    ///   - onZoomIn: The on zoom in tapped action. This should be used to zoom the user in one increment.
    ///   - onZoomOut: The on zoom out tapped action. This should be used to zoom the user out one increment.
    ///   - showCentering: Whether to show the centering control. This is typically determined by the Map's centering
    /// state.
    ///   - onCenter: The action that occurs when the user taps the centering control. Typically re-centering the user.
    ///   - topCenter: The customizable top center view. This is recommended for navigation alerts (e.g. toast style
    /// notices).
    ///   - topTrailing: The customizable top trailing view. This can be used for custom interactions or metadata views.
    ///   - midLeading: The customizable mid leading view. This can be used for custom interactions or metadata views.
    ///   - bottomTrailing: The customizable bottom leading view. This can be used for custom interactions or metadata
    /// views.
    public init(
        theme: any FerrostarTheme = DefaultFerrostarTheme(),
        speedLimit: Measurement<UnitSpeed>? = nil,
        showZoom: Bool = false,
        onZoomIn: @escaping () -> Void = {},
        onZoomOut: @escaping () -> Void = {},
        showCentering: Bool = false,
        onCenter: @escaping () -> Void = {},
        @ViewBuilder topCenter: @escaping () -> TopCenter = { InfiniteSpacer() },
        @ViewBuilder topTrailing: @escaping () -> TopTrailing = { InfiniteSpacer() },
        @ViewBuilder midLeading: @escaping () -> MidLeading = { InfiniteSpacer() },
        @ViewBuilder bottomTrailing: @escaping () -> BottomTrailing = { InfiniteSpacer() }
    ) {
        self.theme = theme
        self.speedLimit = speedLimit
        self.showZoom = showZoom
        self.onZoomIn = onZoomIn
        self.onZoomOut = onZoomOut
        self.showCentering = showCentering
        self.onCenter = onCenter
        self.topCenter = topCenter
        self.topTrailing = topTrailing
        self.midLeading = midLeading
        self.bottomTrailing = bottomTrailing
    }

    public var body: some View {
        InnerGridView(
            topLeading: {
                if let speedLimit {
                    SpeedLimitView(
                        speedLimit: speedLimit,
                        valueFormatter: theme.speedValueFormatter,
                        unitFormatter: theme.speedWithUnitsFormatter
                    )
                }
            },
            topCenter: { topCenter() },
            topTrailing: { topTrailing() },
            midLeading: { midLeading() },
            midCenter: {
                // This view does not allow center content.
                InfiniteSpacer()
            },
            midTrailing: {
                if showZoom {
                    ZoomButton(onZoomIn: onZoomIn, onZoomOut: onZoomOut)
                        .shadow(radius: 8)
                } else {
                    InfiniteSpacer()
                }
            },
            bottomLeading: {
                if showCentering {
                    FerrostarButton(action: onCenter) {
                        Image(systemName: "location.north.fill")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 18, height: 18)
                    }
                    .shadow(radius: 8)
                } else {
                    InfiniteSpacer()
                }
            },
            bottomCenter: {
                // This view does not allow center content to prevent overlaying the puck.
                InfiniteSpacer()
            },
            bottomTrailing: { bottomTrailing() }
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
            showZoom: true,
            showCentering: true
        )
        .padding(.horizontal, 16)

        RoundedRectangle(cornerRadius: 36)
            .padding(.horizontal, 16)
            .frame(height: 72)
    }
    .background(Color.green)
}
