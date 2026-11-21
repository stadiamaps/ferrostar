import FerrostarCoreFFI
import SwiftUI
import UIKit
import XCTest
@testable import FerrostarSwiftUI

final class InstructionsViewMeasurementTests: XCTestCase {
    @MainActor func testFinalInstructionKeepsItsHeightUntilTheBannerIsRemoved() async throws {
        var measuredSize = CGSize.zero
        let measured = expectation(description: "Initial banner is measured")
        measured.assertForOverFulfill = false
        let size = Binding(get: { measuredSize }, set: {
            measuredSize = $0
            if $0.height > 0 {
                measured.fulfill()
            }
        })
        let host = UIHostingController(rootView: InstructionMeasurementFixture(hasFollowingSteps: true, size: size))
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
        window.rootViewController = host
        window.makeKeyAndVisible()
        defer { window.isHidden = true; window.rootViewController = nil }
        host.view.layoutIfNeeded()
        await fulfillment(of: [measured], timeout: 2)

        // This replaces the drawer's expandable subtree with a single row.
        // Its disappearing child must not reset the new row's measurement.
        host.rootView = InstructionMeasurementFixture(hasFollowingSteps: false, size: size)
        host.view.layoutIfNeeded()
        try await Task.sleep(nanoseconds: 100_000_000)
        XCTAssertGreaterThan(measuredSize.height, 0)

        host.rootView = InstructionMeasurementFixture(hasFollowingSteps: false, isVisible: false, size: size)
        host.view.layoutIfNeeded()
        try await Task.sleep(nanoseconds: 100_000_000)
        XCTAssertEqual(measuredSize, .zero)
    }
}

private struct InstructionMeasurementFixture: View {
    let hasFollowingSteps: Bool
    var isVisible = true
    let size: Binding<CGSize>

    private let instruction = VisualInstruction(
        primaryContent: VisualInstructionContent(text: "Destination", maneuverType: .arrive,
                                                 maneuverModifier: nil, roundaboutExitDegrees: nil,
                                                 laneInfo: nil, exitNumbers: []),
        secondaryContent: nil, subContent: nil, triggerDistanceBeforeManeuver: 100
    )

    var body: some View {
        if isVisible {
            let step = RouteStep(geometry: [], distance: 100, duration: 30, roadName: nil, exits: [],
                                 instruction: "Continue", visualInstructions: [instruction], spokenInstructions: [],
                                 annotations: nil, incidents: [], drivingSide: nil, roundaboutExitNumber: nil)
            InstructionsView(visualInstruction: instruction, remainingSteps: hasFollowingSteps ? [step, step] : [step],
                             sizeWhenNotExpanded: size)
                .frame(maxHeight: .infinity, alignment: .top)
        }
    }
}
