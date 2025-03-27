import CarPlay
import FerrostarCoreFFI
import MapKit
import Testing

@testable import FerrostarCarPlayUI

struct CPManeuverTests {
    @Test("Maneuver init from nil instruction")
    func maneuverNil() async throws {
        let meters = Measurement(value: 1.0, unit: UnitLength.meters)
        let maneuver = CPManeuver.fromFerrostar(nil, stepDuration: 10.0, stepDistance: meters)

        #expect(maneuver == nil)
    }

    @Test("Maneuver init from ferrostar")
    func maneuverInit() async throws {
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

        let maneuver = CPManeuver.fromFerrostar(instruction, stepDuration: 10.0, stepDistance: meters)

        #expect(maneuver?.instructionVariants.first == "Maneuver instruction.")

        #expect(maneuver?.initialTravelEstimates?.distanceRemaining == meters)
        #expect(maneuver?.initialTravelEstimates?.distanceRemainingToDisplay == meters)
        #expect(maneuver?.initialTravelEstimates?.timeRemaining == 10.0)
    }
}
