import TestSupport
import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class DefaultManeuverInstructionViewTests: XCTestCase {
    func testDefaultManeuverInstructionViewUS() {
        assertView {
            DefaultIconographyManeuverInstructionView(
                text: "Merge Left onto Something",
                maneuverType: .merge,
                maneuverModifier: .left,
                distanceFormatter: americanDistanceFormatter,
                distanceToNextManeuver: 1500.0,
                theme: TestingInstructionRowTheme()
            )
            .background(.white)
        }
    }

    func testDefaultManeuverInstructionViewDE() {
        assertView {
            DefaultIconographyManeuverInstructionView(
                text: "Links einfädeln",
                maneuverType: .merge,
                maneuverModifier: .left,
                distanceFormatter: germanDistanceFormatter,
                distanceToNextManeuver: 1500.0,
                theme: TestingInstructionRowTheme()
            )
            .background(.white)
        }
    }
}
