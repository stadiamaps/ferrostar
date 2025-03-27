import CarPlay
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI

extension CPManeuver {
    static func fromFerrostar(_ instruction: VisualInstruction?,
                              stepDuration: TimeInterval,
                              stepDistance: Measurement<UnitLength>) -> CPManeuver?
    {
        guard let instruction else {
            return nil
        }

        let maneuver = CPManeuver()

        // CarPlay take the "initial" estimates and internally tracks the reduction.
        maneuver.initialTravelEstimates = CPTravelEstimates(
            distanceRemaining: stepDistance,
            distanceRemainingToDisplay: stepDistance,
            timeRemaining: stepDuration
        )

        // The instructions. CPManeuver lists them from
        // highest (idx-0) to lowest priority.
        let instructions = [
            instruction.primaryContent.text,
            instruction.secondaryContent?.text,
        ]
        maneuver.instructionVariants = instructions.compactMap { $0 }

        // Display a maneuver image if one could be calculated.
        if let maneuverType = instruction.primaryContent.maneuverType {
            let maneuverModifier = instruction.primaryContent.maneuverModifier
            let maneuverImage = ManeuverUIImage(maneuverType: maneuverType, maneuverModifier: maneuverModifier)
            maneuver.symbolImage = maneuverImage.uiImage
        }

        return maneuver
    }
}
