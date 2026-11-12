import FerrostarCoreFFI
import XCTest
@testable import FerrostarSwiftUI

@available(iOS 16.2, *)
final class FerrostarWidgetProviderTests: XCTestCase {
    func testNewInstructionAtSameDistanceIsForwarded() {
        var received: [VisualInstruction] = []
        let provider = FerrostarWidgetProvider { instruction, _, _ in received.append(instruction) }
        let first = instruction(text: "First Street")
        let next = instruction(text: "Second Street")

        update(provider, instruction: first, distance: 150)
        update(provider, instruction: next, distance: 150)

        XCTAssertEqual(received, [first, next])
    }

    func testNewManeuverWithSameTextIsForwarded() {
        var received: [VisualInstruction] = []
        let provider = FerrostarWidgetProvider { instruction, _, _ in received.append(instruction) }
        let first = instruction(text: "Main Street")
        var next = first
        next.primaryContent.maneuverModifier = .right

        update(provider, instruction: first, distance: 150)
        update(provider, instruction: next, distance: 150)

        XCTAssertEqual(received, [first, next])
    }

    func testSecondaryAndSubInstructionChangesAreForwarded() {
        var received: [VisualInstruction] = []
        let provider = FerrostarWidgetProvider { instruction, _, _ in received.append(instruction) }
        let first = instruction(text: "Main Street")
        var secondary = first
        secondary.secondaryContent = instruction(text: "Keep left").primaryContent
        var sub = secondary
        sub.subContent = instruction(text: "Then turn left").primaryContent

        for instruction in [first, secondary, sub, first] {
            update(provider, instruction: instruction, distance: 150)
        }

        XCTAssertEqual(received, [first, secondary, sub, first])
    }

    func testUnchangedInstructionStillUsesEachDistanceThreshold() {
        let instruction = instruction(text: "Main Street")
        let distancesAndThresholds: [(Double, Double)] = [
            (20, 5), (70, 10), (150, 15), (300, 25), (700, 50), (1500, 100),
        ]

        for (distance, threshold) in distancesAndThresholds {
            var received: [Double] = []
            let provider = FerrostarWidgetProvider { _, _, progress in
                received.append(progress.distanceToNextManeuver)
            }
            let initialDistance = distance + threshold - 1

            update(provider, instruction: instruction, distance: initialDistance)
            update(provider, instruction: instruction, distance: distance)
            XCTAssertEqual(received, [initialDistance], "Below the threshold at \(distance) metres")

            update(provider, instruction: instruction, distance: distance - 1)
            XCTAssertEqual(received, [initialDistance, distance - 1], "At the threshold at \(distance) metres")
        }
    }

    func testIncreasingDistanceStillTriggersAnUpdate() {
        var received: [Double] = []
        let provider = FerrostarWidgetProvider { _, _, progress in
            received.append(progress.distanceToNextManeuver)
        }
        let instruction = instruction(text: "Main Street")

        update(provider, instruction: instruction, distance: 150)
        update(provider, instruction: instruction, distance: 160)
        update(provider, instruction: instruction, distance: 165)

        XCTAssertEqual(received, [150, 165])
    }

    func testNewInstructionResetsTheDistanceBaseline() {
        var received: [Double] = []
        let provider = FerrostarWidgetProvider { _, _, progress in
            received.append(progress.distanceToNextManeuver)
        }
        let first = instruction(text: "First Street")
        let next = instruction(text: "Second Street")

        update(provider, instruction: first, distance: 300)
        update(provider, instruction: next, distance: 290)
        update(provider, instruction: next, distance: 270)
        update(provider, instruction: next, distance: 265)

        XCTAssertEqual(received, [300, 290, 265])
    }

    func testRepeatedInstructionAtSameDistanceIsThrottled() {
        var updateCount = 0
        let provider = FerrostarWidgetProvider { _, _, _ in updateCount += 1 }
        let instruction = instruction(text: "Main Street")

        update(provider, instruction: instruction, distance: 150)
        update(provider, instruction: instruction, distance: 150)

        XCTAssertEqual(updateCount, 1)
    }

    func testTerminateAllowsTheSameInstructionAndDistanceAgain() {
        var updateCount = 0
        let provider = FerrostarWidgetProvider { _, _, _ in updateCount += 1 }
        let instruction = instruction(text: "Main Street")

        update(provider, instruction: instruction, distance: 150)
        provider.terminate()
        update(provider, instruction: instruction, distance: 150)

        XCTAssertEqual(updateCount, 2)
    }

    func testFailedActivityRequestAllowsTheSameInstructionToRetry() {
        enum RequestError: Error { case failed }
        var attemptCount = 0
        var received: [VisualInstruction] = []
        let provider = FerrostarWidgetProvider { instruction, _, _ in
            attemptCount += 1
            if attemptCount == 1 {
                throw RequestError.failed
            }
            received.append(instruction)
        }
        let instruction = instruction(text: "Main Street")

        update(provider, instruction: instruction, distance: 150)
        update(provider, instruction: instruction, distance: 150)
        update(provider, instruction: instruction, distance: 150)

        XCTAssertEqual(attemptCount, 2)
        XCTAssertEqual(received, [instruction])
    }

    private func instruction(text: String) -> VisualInstruction {
        VisualInstruction(
            primaryContent: .init(
                text: text,
                maneuverType: .turn,
                maneuverModifier: .left,
                roundaboutExitDegrees: nil,
                laneInfo: nil,
                exitNumbers: []
            ),
            secondaryContent: nil,
            subContent: nil,
            triggerDistanceBeforeManeuver: 200
        )
    }

    private func update(
        _ provider: FerrostarWidgetProvider,
        instruction: VisualInstruction,
        distance: Double
    ) {
        provider.update(
            visualInstruction: instruction,
            spokenInstruction: nil,
            tripProgress: TripProgress(
                distanceToNextManeuver: distance,
                distanceRemaining: distance,
                durationRemaining: 60
            )
        )
    }
}
