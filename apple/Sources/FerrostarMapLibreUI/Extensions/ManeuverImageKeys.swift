import FerrostarCoreFFI
import Foundation

// TODO: See if this could probably be in the core reasonably?
extension ManeuverType {
    var iconKey: String {
        switch self {
        case .turn:
            "turn"
        case .newName:
            "new name"
        case .depart:
            "depart"
        case .arrive:
            "arrive"
        case .merge:
            "merge"
        case .onRamp:
            "on ramp"
        case .offRamp:
            "off ramp"
        case .fork:
            "fork"
        case .endOfRoad:
            "end of road"
        case .continue:
            "continue"
        case .roundabout:
            "roundabout"
        case .rotary:
            "rotary"
        case .roundaboutTurn:
            "roundabout turn"
        case .notification:
            "notification"
        case .exitRoundabout:
            "exit roundabout"
        case .exitRotary:
            "exit rotary"
        }
    }
}

// TODO: See if this could probably be in the core reasonably?
extension ManeuverModifier {
    var iconKey: String {
        switch self {
        case .uTurn:
            "uturn"
        case .sharpRight:
            "sharp right"
        case .right:
            "right"
        case .slightRight:
            "slight right"
        case .straight:
            "straight"
        case .slightLeft:
            "slight left"
        case .left:
            "left"
        case .sharpLeft:
            "sharp left"
        }
    }
}
