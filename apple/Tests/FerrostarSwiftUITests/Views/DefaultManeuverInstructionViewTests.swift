import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class DefaultManeuverInstructionViewTests: XCTestCase {
    func testDefaultManeuverInstructionView() {
        assertView {
            DefaultManeuverInstructionView(
                text: "Merge Left onto Something",
                maneuverType: .merge,
                maneuverModifier: .left,
                distanceToNextManeuver: 500.0,
                theme: TestingInstructionRowTheme()
            )
            .background(.white)
        }
    }
}
