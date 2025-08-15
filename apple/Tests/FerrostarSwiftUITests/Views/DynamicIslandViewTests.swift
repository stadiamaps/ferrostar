import TestSupport
import XCTest
@testable import FerrostarCore
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class DynamicIslandViewTests: XCTestCase {
    func testLiveActivityManeuverImage() {
        assertView {
            LiveActivityManeuverImage(
                state: .init(
                    instruction: VisualInstructionFactory().build(),
                    distanceToNextManeuver: 123
                )
            )
            .padding()
        }
    }

    func testLiveActivityView() {
        assertView {
            LiveActivityView(
                state: .init(
                    instruction: VisualInstructionFactory().build(),
                    distanceToNextManeuver: 123
                )
            )
            .padding()
        }
    }
}
