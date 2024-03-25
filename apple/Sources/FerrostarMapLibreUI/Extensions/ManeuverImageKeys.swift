import Foundation
import FerrostarCoreFFI

// TODO: See if this could probably be in the core reasonably?
extension ManeuverType {
    
    var iconKey: String {
        switch self {
        case .turn:
            return "turn"
        case .newName:
            return "new name"
        case .depart:
            return "depart"
        case .arrive:
            return "arrive"
        case .merge:
            return "merge"
        case .onRamp:
            return "on ramp"
        case .offRamp:
            return "off ramp"
        case .fork:
            return "fork"
        case .endOfRoad:
            return "end of road"
        case .continue:
            return "continue"
        case .roundabout:
            return "roundabout"
        case .rotary:
            return "rotary"
        case .roundaboutTurn:
            return "roundabout turn"
        case .notification:
            return "notification"
        case .exitRoundabout:
            return "exit roundabout"
        case .exitRotary:
            return "exit rotary"
        }
    }
}

// TODO: See if this could probably be in the core reasonably?
extension ManeuverModifier {
    
    var iconKey: String {
        switch self {
        case .uTurn:
            return "uturn"
        case .sharpRight:
            return "sharp right"
        case .right:
            return "right"
        case .slightRight:
            return "slight right"
        case .straight:
            return "straight"
        case .slightLeft:
            return "slight left"
        case .left:
            return "left"
        case .sharpLeft:
            return "sharp left"
        }
    }
}
