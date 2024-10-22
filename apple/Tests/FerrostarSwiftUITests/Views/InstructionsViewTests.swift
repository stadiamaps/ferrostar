import XCTest
@testable import FerrostarCore
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
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    secondaryContent: VisualInstructionContent(
                        text: "Merge onto Hwy 123",
                        maneuverType: .merge,
                        maneuverModifier: .right,
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    subContent: nil,
                    triggerDistanceBeforeManeuver: 123
                ),
                distanceFormatter: americanDistanceFormatter
            )
            .padding()
        }
    }

    func testInstructionsView_darkMode() {
        assertView(colorScheme: .dark) {
            InstructionsView(
                visualInstruction: VisualInstruction(
                    primaryContent: VisualInstructionContent(
                        text: "Turn right on Something Dr.",
                        maneuverType: .turn,
                        maneuverModifier: .right,
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    secondaryContent: VisualInstructionContent(
                        text: "Merge onto Hwy 123",
                        maneuverType: .merge,
                        maneuverModifier: .right,
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    subContent: nil,
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
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    secondaryContent: nil,
                    subContent: nil,
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
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    secondaryContent: nil,
                    subContent: nil,
                    triggerDistanceBeforeManeuver: 123
                ),
                distanceFormatter: americanDistanceFormatter,
                remainingSteps: RouteStepFactory().buildMany(3)
            )
            .padding()
        }
    }

    func testSingularInstructionsViewWithPill_darkMode() {
        assertView(colorScheme: .dark) {
            InstructionsView(
                visualInstruction: VisualInstruction(
                    primaryContent: VisualInstructionContent(
                        text: "Use the second exit to leave the roundabout.",
                        maneuverType: .rotary,
                        maneuverModifier: .slightRight,
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    secondaryContent: nil,
                    subContent: nil,
                    triggerDistanceBeforeManeuver: 123
                ),
                distanceFormatter: americanDistanceFormatter,
                remainingSteps: RouteStepFactory().buildMany(3)
            )
            .padding()
        }
    }

    func testExpandedInstructionsView() {
        assertView {
            InstructionsView(
                visualInstruction: VisualInstruction(
                    primaryContent: VisualInstructionContent(
                        text: "Use the second exit to leave the roundabout.",
                        maneuverType: .rotary,
                        maneuverModifier: .slightRight,
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    secondaryContent: nil,
                    subContent: nil,
                    triggerDistanceBeforeManeuver: 123
                ),
                distanceFormatter: americanDistanceFormatter,
                remainingSteps: RouteStepFactory().buildMany(3),
                isExpanded: .constant(true)
            )
            .padding()
        }
    }

    func testExpandedInstructionsView_darkMode() {
        assertView(colorScheme: .dark) {
            InstructionsView(
                visualInstruction: VisualInstruction(
                    primaryContent: VisualInstructionContent(
                        text: "Use the second exit to leave the roundabout.",
                        maneuverType: .rotary,
                        maneuverModifier: .slightRight,
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    secondaryContent: nil,
                    subContent: nil,
                    triggerDistanceBeforeManeuver: 123
                ),
                distanceFormatter: americanDistanceFormatter,
                remainingSteps: RouteStepFactory().buildMany(3),
                isExpanded: .constant(true)
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
                        roundaboutExitDegrees: nil,
                        laneInfo: nil
                    ),
                    secondaryContent: nil,
                    subContent: nil,
                    triggerDistanceBeforeManeuver: 123
                ),
                distanceFormatter: germanDistanceFormatter,
                distanceToNextManeuver: 152.4
            )
            .padding()
        }
    }
}
