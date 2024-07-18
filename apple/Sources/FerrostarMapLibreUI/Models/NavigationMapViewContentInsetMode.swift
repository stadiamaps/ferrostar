import SwiftUI
import UIKit

public enum NavigationMapViewContentInsetMode {
    
    /// A predefined mode for landscape navigation map views
    /// where the user location should appear toward the bottom of the map.
    ///
    /// This is used to accommodate a left InstructionView
    case landscape(within: GeometryProxy, verticalPct: CGFloat = 0.75, horizontalPct: CGFloat = 0.5)

    /// A predefined mode for landscape navigation map views
    /// where the user location should appear toward the bottom of the map.
    ///
    /// This is used to accommodate a top InstructionView
    case portrait(within: GeometryProxy, verticalPct: CGFloat = 0.75)

    /// Custom edge insets to manually control where the center of the map is.
    case edgeInset(UIEdgeInsets)

    var uiEdgeInsets: UIEdgeInsets {
        switch self {
        case let .landscape(geometry, verticalPct, horizontalPct):
            let top = geometry.size.height * verticalPct
            let leading = geometry.size.width * horizontalPct

            return UIEdgeInsets(top: top, left: leading, bottom: 0, right: 0)
        case let .portrait(geometry, verticalPct):
            let top = geometry.size.height * verticalPct

            return UIEdgeInsets(top: top, left: 0, bottom: 0, right: 0)
        case let .edgeInset(uIEdgeInsets):
            return uIEdgeInsets
        }
    }
}
