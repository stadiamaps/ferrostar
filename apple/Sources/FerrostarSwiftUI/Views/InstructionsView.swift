import CoreLocation
import FerrostarCoreFFI
import MapKit
import SwiftUI

/// The core instruction view. This displays the current step with it's primary and secondary instruction.
public struct InstructionsView: View {
    private let visualInstruction: VisualInstruction
    private let distanceToNextManeuver: CLLocationDistance?
    private let distanceFormatter: Formatter

    private let primaryRowTheme: InstructionRowTheme
    private let secondaryRowTheme: InstructionRowTheme
    private var hasSecondary: Bool {
        visualInstruction.secondaryContent != nil
    }

    private let showPillControl: Bool

    /// Create a visual instruction banner view. This view automatically displays the secondary
    /// instruction if there is one.
    ///
    /// - Parameters:
    ///   - visualInstruction: The visual instruction to display.
    ///   - distanceFormatter: The formatter which controls distance localization.
    ///   - distanceToNextManeuver: The distance remaining for the step.
    ///   - primaryRowTheme: The theme for the primary instruction.
    ///   - secondaryRowTheme: The theme for the secondary instruction.
    ///   - showPillControl: If true, shows a pill control (to indicate an action/expansion).
    public init(
        visualInstruction: VisualInstruction,
        distanceFormatter: Formatter = DefaultFormatters.distanceFormatter,
        distanceToNextManeuver: CLLocationDistance? = nil,
        primaryRowTheme: InstructionRowTheme = DefaultInstructionRowTheme(),
        secondaryRowTheme: InstructionRowTheme = DefaultSecondaryInstructionRowTheme(),
        showPillControl: Bool = false
    ) {
        self.visualInstruction = visualInstruction
        self.distanceFormatter = distanceFormatter
        self.distanceToNextManeuver = distanceToNextManeuver
        self.primaryRowTheme = primaryRowTheme
        self.secondaryRowTheme = secondaryRowTheme
        self.showPillControl = showPillControl
    }

    public var body: some View {
        VStack {
            DefaultIconographyManeuverInstructionView(
                text: visualInstruction.primaryContent.text,
                maneuverType: visualInstruction.primaryContent.maneuverType,
                maneuverModifier: visualInstruction.primaryContent.maneuverModifier,
                distanceFormatter: distanceFormatter,
                distanceToNextManeuver: distanceToNextManeuver,
                theme: primaryRowTheme
            )
            .font(.title2.bold())
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 0)

            if let secondaryContent = visualInstruction.secondaryContent {
                VStack {
                    DefaultIconographyManeuverInstructionView(
                        text: secondaryContent.text,
                        maneuverType: secondaryContent.maneuverType,
                        maneuverModifier: secondaryContent.maneuverModifier,
                        distanceFormatter: distanceFormatter,
                        theme: secondaryRowTheme
                    )
                    .padding(.horizontal, 16)
                    .padding(.top, 8)

                    // TODO: Show the pill when interactivity is enabled
                    pillControl(isActive: showPillControl)
                }
                .background(secondaryRowTheme.backgroundColor)
            } else {
                // TODO: Show the pill when interactivity is enabled
                pillControl(isActive: showPillControl)
            }
        }
        .background(primaryRowTheme.backgroundColor)
        .clipShape(.rect(cornerRadius: 12))
        .shadow(radius: 12)
    }

    /// The pill control that is shown at the bottom of the Instructions View.
    @ViewBuilder fileprivate func pillControl(isActive: Bool) -> some View {
        RoundedRectangle(cornerRadius: 3)
            .frame(width: 24, height: isActive ? 6 : 0)
            .opacity(isActive ? 0.1 : 0.0)
            .padding(.bottom, 8)
    }
}

#Preview {
    let germanFormatter = MKDistanceFormatter()
    germanFormatter.locale = Locale(identifier: "de_DE")
    germanFormatter.units = .metric

    return VStack(spacing: 16) {
        InstructionsView(
            visualInstruction: VisualInstruction(
                primaryContent: VisualInstructionContent(
                    text: "Turn right on Something Dr.",
                    maneuverType: .turn,
                    maneuverModifier: .right,
                    roundaboutExitDegrees: nil
                ),
                secondaryContent: VisualInstructionContent(
                    text: "Merge onto Hwy 123",
                    maneuverType: .merge,
                    maneuverModifier: .right,
                    roundaboutExitDegrees: nil
                ),
                triggerDistanceBeforeManeuver: 123
            )
        )

        InstructionsView(
            visualInstruction: VisualInstruction(
                primaryContent: VisualInstructionContent(
                    text: "Use the second exit to leave the roundabout.",
                    maneuverType: .rotary,
                    maneuverModifier: .slightRight,
                    roundaboutExitDegrees: nil
                ),
                secondaryContent: nil,
                triggerDistanceBeforeManeuver: 123
            )
        )

        InstructionsView(
            visualInstruction: VisualInstruction(
                primaryContent: VisualInstructionContent(
                    text: "Links einfädeln.",
                    maneuverType: .merge,
                    maneuverModifier: .slightLeft,
                    roundaboutExitDegrees: nil
                ),
                secondaryContent: nil,
                triggerDistanceBeforeManeuver: 123
            ),
            distanceFormatter: germanFormatter,
            distanceToNextManeuver: 1500.0,
            showPillControl: true
        )

        InstructionsView(
            visualInstruction: VisualInstruction(
                primaryContent: VisualInstructionContent(
                    text: "Turn right on Something Dr.",
                    maneuverType: .turn,
                    maneuverModifier: .right,
                    roundaboutExitDegrees: nil
                ),
                secondaryContent: VisualInstructionContent(
                    text: "Merge onto Hwy 123",
                    maneuverType: .merge,
                    maneuverModifier: .right,
                    roundaboutExitDegrees: nil
                ),
                triggerDistanceBeforeManeuver: 123
            ),
            showPillControl: true
        )

        Spacer()
    }
    .padding()
    .background(Color.green)
}
