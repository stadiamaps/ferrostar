import FerrostarCoreFFI
import SwiftUI

/// The core instruction view. This displays the current step with it's primary and secondary instruction.
struct InstructionsView: View {
    private let visualInstruction: VisualInstruction
    private let primaryRowTheme: InstructionRowTheme
    private let secondaryRowTheme: InstructionRowTheme
    private var hasSecondary: Bool {
        visualInstruction.secondaryContent != nil
    }

    /// Create a visual instruction banner view. This view automatically displays the secondary
    /// instruction if there is one.
    ///
    /// - Parameters:
    ///   - visualInstruction: The visual instruction to display.
    ///   - primaryRowTheme: The theme for the primary instruction.
    ///   - secondaryRowTheme: The theme for the secondary instruction.
    public init(
        visualInstruction: VisualInstruction,
        primaryRowTheme: InstructionRowTheme = DefaultInstructionRowTheme(),
        secondaryRowTheme: InstructionRowTheme = DefaultSecondaryInstructionRowTheme(),
        onTapOrDrag: () -> Void = { }
    ) {
        self.visualInstruction = visualInstruction
        self.primaryRowTheme = primaryRowTheme
        self.secondaryRowTheme = secondaryRowTheme
    }

    var body: some View {
        VStack {
            DefaultManeuverInstructionView(
                text: visualInstruction.primaryContent.text,
                maneuverType: visualInstruction.primaryContent.maneuverType,
                maneuverModifier: .left,
                distanceRemaining: "15 km", // TODO: Dynamic distance to step
                theme: primaryRowTheme
            )
            .font(.title2.bold())
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 0)

            if let secondaryContent = visualInstruction.secondaryContent {
                VStack {
                    DefaultManeuverInstructionView(
                        text: secondaryContent.text,
                        maneuverType: secondaryContent.maneuverType,
                        maneuverModifier: secondaryContent.maneuverModifier,
                        distanceRemaining: "500 m", // TODO: Dynamic distance to step
                        theme: secondaryRowTheme
                    )
                    .padding(.horizontal, 16)
                    .padding(.top, 8)

                    RoundedRectangle(cornerRadius: 3)
                        .frame(width: 24, height: 6)
                        .opacity(0.1)
                        .padding(.bottom, 8)
                }
                .background(.gray.opacity(0.2))
            } else {
                // TODO: Do we want to add a specific drag/tap handler? Or just let the a global view modifier handle it.
                RoundedRectangle(cornerRadius: 3)
                    .frame(width: 24, height: 6)
                    .opacity(0.1)
                    .padding(.bottom, 8)
            }
        }
        .background(Color.white)
        .clipShape(.rect(cornerRadius: 12))
        .padding()
        .shadow(radius: 12)
    }
}

#Preview {
    VStack {
        InstructionsView(
            visualInstruction: PreviewModels.visualInstruction
        )

        // TODO: This instruction doesn't match :o
        InstructionsView(
            visualInstruction: PreviewModels.reducedVisualInstructions
        )

        Spacer()
    }
    .background(Color.green)
}

#if DEBUG
    fileprivate class PreviewModels {
        static let visualInstruction = VisualInstruction(
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

        static let reducedVisualInstructions = VisualInstruction(
            primaryContent: VisualInstructionContent(
                text: "Use the second exit to leave the roundabout.",
                maneuverType: .rotary,
                maneuverModifier: .slightRight,
                roundaboutExitDegrees: nil
            ),
            secondaryContent: nil,
            triggerDistanceBeforeManeuver: 123
        )
    }
#endif
