import UIKit

public enum NavigationMapViewContentInsetMode {
    
    /// A predefined mode for landscape navigation map views
    /// where the user location should appear toward the bottom of the map.
    ///
    /// This is used to accomidate a top InstructionView
    case landscape
    
    /// A predefined mode for landscape navigation map views
    /// where the user location should appear toward the bottom of the map.
    ///
    /// This is used to accomidate a left InstructionView
    case portrait
    
    case edgeInset(UIEdgeInsets)
    
    var uiEdgeInsets: UIEdgeInsets {
        switch self {
        case .landscape:
            // TODO: Polish this based on actual screen size.
            return UIEdgeInsets(top: 0, left: 450, bottom: 0, right: 0)
        case .portrait:
            // TODO: Polish this based on actual screen size.
            return UIEdgeInsets(top: 450, left: 0, bottom: 0, right: 0)
        case .edgeInset(let uIEdgeInsets):
            return uIEdgeInsets
        }
    }
}
