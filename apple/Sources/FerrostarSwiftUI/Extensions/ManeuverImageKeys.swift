import FerrostarCoreFFI
import Foundation

extension ManeuverType {
    /// The the string value representation of the ManeuverType.
    ///
    /// This string matches the OSRM json value and is used to load the maneuver icon.
    var stringValue: String {
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

extension ManeuverModifier {
    /// The the string value representation of the ManeuverModifier.
    ///
    /// This string matches the OSRM json value and is used to load the maneuver icon.
    var stringValue: String {
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
