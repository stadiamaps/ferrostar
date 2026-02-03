import CarPlay
import FerrostarCoreFFI
import MapKit
import Testing
@testable import FerrostarCarPlayUI

struct CPManeuverTests {
    @Test("Maneuver init from ferrostar")
    func maneuverInit() {
        let instruction = VisualInstruction(
            primaryContent: .init(
                text: "Maneuver instruction.",
                maneuverType: .turn,
                maneuverModifier: .sharpLeft,
                roundaboutExitDegrees: nil,
                laneInfo: [],
                exitNumbers: []
            ),
            secondaryContent: nil,
            subContent: nil,
            triggerDistanceBeforeManeuver: 0.0
        )
        let meters = Measurement(value: 1.0, unit: UnitLength.meters)

        let maneuver = instruction.maneuver(stepDuration: 10.0, stepDistance: meters)

        #expect(maneuver.instructionVariants.first == "Maneuver instruction.")

        #expect(maneuver.initialTravelEstimates?.distanceRemaining == meters)
        if #available(iOS 17.4, *) {
            #expect(maneuver.initialTravelEstimates?.distanceRemainingToDisplay == meters)
        }
        #expect(maneuver.initialTravelEstimates?.timeRemaining == 10.0)
    }
}
