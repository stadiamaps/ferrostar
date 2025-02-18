import CarPlay
import FerrostarCore
import FerrostarSwiftUI

extension NavigationState {
    /// The CarPlay CPManeuver. This is used to show
    var cpManeuver: CPManeuver {
        var maneuver = CPManeuver()

        // The instructions. CPManeuver lists them from
        // highest (idx-0) to lowest priority.
        let instructions = [
            currentVisualInstruction?.primaryContent.text,
            currentVisualInstruction?.secondaryContent?.text,
        ]
        maneuver.instructionVariants = instructions.compactMap { $0 }

        // Display a manevuer image if one could be calculated.
        if let maneuverType = currentVisualInstruction?.primaryContent.maneuverType {
            let maneuverModifier = currentVisualInstruction?.primaryContent.maneuverModifier
            let maneuverImage = ManeuverUIImage(maneuverType: maneuverType, maneuverModifier: maneuverModifier)
            maneuver.symbolImage = maneuverImage.uiImage
        }
    }
}
