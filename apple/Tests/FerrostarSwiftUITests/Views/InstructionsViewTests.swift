import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class InstructionsViewTests: XCTestCase {
    func testInstructionsView() {
        assertView {
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
                distanceFormatter: americanDistanceFormatter
            )
            .padding()
        }
    }

    func testSingularInstructionsView() {
        assertView {
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
                ),
                distanceFormatter: americanDistanceFormatter
            )
            .padding()
        }
    }

    func testSingularInstructionsViewWithPill() {
        assertView {
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
                ),
                distanceFormatter: americanDistanceFormatter,
                showPillControl: true
            )
            .padding()
        }
    }

    func testFormattingDE() {
        assertView {
            InstructionsView(
                visualInstruction: VisualInstruction(
                    primaryContent: VisualInstructionContent(
                        text: "Links einfädeln",
                        maneuverType: .turn,
                        maneuverModifier: .left,
                        roundaboutExitDegrees: nil
                    ),
                    secondaryContent: nil,
                    triggerDistanceBeforeManeuver: 123
                ),
                distanceFormatter: germanDistanceFormatter,
                distanceToNextManeuver: 152.4
            )
            .padding()
        }
    }
}
