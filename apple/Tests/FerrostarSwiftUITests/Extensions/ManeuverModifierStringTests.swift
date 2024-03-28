import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ManeuverModifierStringTests: XCTestCase {
    func testManeuverModifierUTurn() {
        XCTAssertEqual(ManeuverModifier.uTurn.stringValue, "uturn")
    }

    func testManeuverModifierSharpRight() {
        XCTAssertEqual(ManeuverModifier.sharpRight.stringValue, "sharp right")
    }

    func testManeuverModifierRight() {
        XCTAssertEqual(ManeuverModifier.right.stringValue, "right")
    }

    func testManeuverModifierSlightRight() {
        XCTAssertEqual(ManeuverModifier.slightRight.stringValue, "slight right")
    }

    func testManeuverModifierStraight() {
        XCTAssertEqual(ManeuverModifier.straight.stringValue, "straight")
    }

    func testManeuverModifierSlightLeft() {
        XCTAssertEqual(ManeuverModifier.slightLeft.stringValue, "slight left")
    }

    func testManeuverModifierLeft() {
        XCTAssertEqual(ManeuverModifier.left.stringValue, "left")
    }

    func testManeuverModifierSharpLeft() {
        XCTAssertEqual(ManeuverModifier.sharpLeft.stringValue, "sharp left")
    }
}
