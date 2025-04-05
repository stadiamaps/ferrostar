import SwiftUI
import UIKit

public enum NavigationMapViewContentInsetMode {
    /// Dynamically determined insets suitable for landscape orientation,
    /// where the user location indicator should appear toward the bottom right of the screen.
    ///
    /// This mode is used to accommodate an InstructionView in a separate column, left of the content area.
    ///
    /// - Parameter within : The `MapView`'s geometry
    /// - Parameter verticalPct : How far "down" to inset the MapView overlay content. A higher number positions content
    /// lower.
    /// - Parameter horizontalPct : How far "right" to inset the MapView overlay content. A higher number positions
    /// content farther right.
    case landscape(within: GeometryProxy, verticalPct: CGFloat = 0.60, horizontalPct: CGFloat = 0.5)

    /// Dynamically determined insets suitable for portrait orientation,
    /// where the user location indicator should appear toward the bottom of the screen.
    ///
    /// This mode is used to accommodate an InstructionView at the top of the MapView, in a single column with the
    /// content area.
    ///
    /// - Parameter within : The `MapView`'s geometry
    /// FIXME
    /// - Parameter verticalPct : How far "down" to inset the MapView overlay content. A higher number positions content
    /// lower.
    /// - Parameter minHeight : The minimum height (in points) of the content area. The content area could be larger
    /// than this on sufficiently tall screens depending on `verticalPct`.
    case portrait(within: GeometryProxy, minHeight: CGFloat = 270)

    /// Static edge insets to manually control where the center of the map is.
    case edgeInset(UIEdgeInsets)

    var uiEdgeInsets: UIEdgeInsets {
        switch self {
        case let .landscape(geometry, verticalPct, horizontalPct):
            let top = geometry.size.height * verticalPct
            let leading = geometry.size.width * horizontalPct

            return UIEdgeInsets(top: top, left: leading, bottom: 0, right: 0)
        case let .portrait(geometry, minVertical):
            let top = geometry.size.height - minVertical

            return UIEdgeInsets(top: top, left: 0, bottom: 0, right: 0)
        case let .edgeInset(uIEdgeInsets):
            return uIEdgeInsets
        }
    }
}

/// Bundle of content inset modes for landscape and portrait orientations.
public struct NavigationMapViewContentInsetBundle {
    public let landscape: (GeometryProxy) -> NavigationMapViewContentInsetMode
    public let portrait: (GeometryProxy) -> NavigationMapViewContentInsetMode

    /// Create a content inset bundle that handles the NavigationMapView's content inset modes for landscape and
    /// portrait
    ///
    /// - Parameters:
    ///   - landscape: A custom content inset mode for landscape orientation or default:
    /// ``NavigationMapViewContentInsetMode.landscape``.
    ///   - portrait: A custom content inset mode for portrait orientation or default:
    /// ``NavigationMapViewContentInsetMode.portrait``.
    public init(
        landscape: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode = { .landscape(within: $0) },
        portrait: @escaping (GeometryProxy) -> NavigationMapViewContentInsetMode = { .portrait(within: $0) }
    ) {
        self.landscape = landscape
        self.portrait = portrait
    }

    public func dynamic(_ orientation: UIDeviceOrientation) -> (GeometryProxy) -> NavigationMapViewContentInsetMode {
        switch orientation {
        case .landscapeLeft, .landscapeRight:
            landscape
        default:
            portrait
        }
    }
}
