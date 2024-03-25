import FerrostarCoreFFI
import SwiftUI

/// The Default Themed Maneuver Instruction View.
///
/// This view will display the maneuver icon if one exists.
struct DefaultManeuverInstructionView: View {
    private let text: String
    private let maneuverType: ManeuverType?
    private let maneuverModifier: ManeuverModifier?
    private let distanceRemaining: String?
    private let theme: InstructionRowTheme

    /// Initialize a manuever instruction view that includes a leading icon.
    /// As an HStack, this view automatically corrects for .rightToLeft languages.
    ///
    /// - Parameters:
    ///   - text: The maneuver instruction.
    ///   - maneuverType: The maneuver type defines the behavior.
    ///   - maneuverModifier: The maneuver modifier defines the direction.
    ///   - distanceRemaining: A string that should represent the localized distance remaining.
    public init(
        text: String,
        maneuverType: ManeuverType?,
        maneuverModifier: ManeuverModifier?,
        distanceRemaining: String? = nil,
        theme: InstructionRowTheme = DefaultInstructionRowTheme()
    ) {
        self.text = text
        self.maneuverType = maneuverType
        self.maneuverModifier = maneuverModifier
        self.distanceRemaining = distanceRemaining
        self.theme = theme
    }

    var body: some View {
        ManeuverInstructionView(
            text: text,
            distanceRemaining: distanceRemaining,
            theme: theme
        ) {
            if let maneuverType {
                ManeuverImage(
                    maneuverType: maneuverType,
                    maneuverModifier: maneuverModifier
                )
                .frame(maxWidth: 48)
            }
        }
    }
}

#Preview {
    DefaultManeuverInstructionView(
        text: "Merge Left onto Something",
        maneuverType: .merge,
        maneuverModifier: .left,
        distanceRemaining: "500 m"
    )
}
