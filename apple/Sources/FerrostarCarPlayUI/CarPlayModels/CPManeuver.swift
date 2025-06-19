import CarPlay
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI

private let InstructionKey = "com.stadiamaps.ferrostar.instruction"

extension CPManeuver {
    convenience init(
        initialTravelEstimates: CPTravelEstimates,
        instructionVariants: [String],
        symbolImage: UIImage?,
        visualInstruction: VisualInstruction
    ) {
        self.init()
        self.initialTravelEstimates = initialTravelEstimates
        self.instructionVariants = instructionVariants
        self.symbolImage = symbolImage
        userInfo = [InstructionKey: visualInstruction]
    }

    public var visualInstruction: VisualInstruction? {
        guard let info = userInfo as? [String: Any] else { return nil }
        return info[InstructionKey] as? VisualInstruction
    }
}

extension VisualInstruction {
    func maneuver(stepDuration: TimeInterval, stepDistance: Measurement<UnitLength>) -> CPManeuver {
        // CarPlay take the "initial" estimates and internally tracks the reduction.
        let initialTravelEstimates =
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

        // Display a maneuver image if one could be calculated.
        var symbolImage: UIImage?
        if let maneuverType = primaryContent.maneuverType {
            let maneuverModifier = primaryContent.maneuverModifier
            let maneuverImage = ManeuverUIImage(maneuverType: maneuverType, maneuverModifier: maneuverModifier)
            symbolImage = maneuverImage.uiImage
        }

        return CPManeuver(
            initialTravelEstimates: initialTravelEstimates,
            instructionVariants: instructions.compactMap { $0 },
            symbolImage: symbolImage,
            visualInstruction: self
        )
    }
}
