import ActivityKit
import CoreLocation
import FerrostarCoreFFI

struct TripActivityAttributes: ActivityAttributes {
    typealias ContentState = InstructionState

    struct InstructionState: Codable, Hashable {
        let instruction: VisualInstruction
        let distanceToNextManeuver: CLLocationDistance?
    }
}
