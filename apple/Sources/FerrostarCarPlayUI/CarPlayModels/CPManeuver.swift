import CarPlay
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI

extension VisualInstruction {
    func maneuver(stepDuration: TimeInterval, stepDistance: Measurement<UnitLength>) -> CPManeuver {
        let maneuver = CPManeuver()

        // CarPlay take the "initial" estimates and internally tracks the reduction.
        maneuver.initialTravelEstimates =
            if #available(iOS 17.4, *) {
                CPTravelEstimates(
                    distanceRemaining: stepDistance,
                    distanceRemainingToDisplay: stepDistance,
                    timeRemaining: stepDuration
                )
            } else {
                CPTravelEstimates(distanceRemaining: stepDistance, timeRemaining: stepDuration)
            }

        // The instructions. CPManeuver lists them from
        // highest (idx-0) to lowest priority.
        let instructions = [
            primaryContent.text,
            secondaryContent?.text,
        ]
        maneuver.instructionVariants = instructions.compactMap { $0 }

        // Display a maneuver image if one could be calculated.
        if let maneuverType = primaryContent.maneuverType {
            let maneuverModifier = primaryContent.maneuverModifier
            let maneuverImage = ManeuverUIImage(maneuverType: maneuverType, maneuverModifier: maneuverModifier)
            maneuver.symbolImage = maneuverImage.uiImage
        }

        return maneuver
    }
}
