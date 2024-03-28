import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ManeuverTypeStringTests: XCTestCase {
    func testManeuverTypeTurn() {
        XCTAssertEqual(ManeuverType.turn.stringValue, "turn")
    }

    func testManeuverTypeNewName() {
        XCTAssertEqual(ManeuverType.newName.stringValue, "new name")
    }

    func testManeuverTypeDepart() {
        XCTAssertEqual(ManeuverType.depart.stringValue, "depart")
    }

    func testManeuverTypeArrive() {
        XCTAssertEqual(ManeuverType.arrive.stringValue, "arrive")
    }

    func testManeuverTypeMerge() {
        XCTAssertEqual(ManeuverType.merge.stringValue, "merge")
    }

    func testManeuverTypeOnRamp() {
        XCTAssertEqual(ManeuverType.onRamp.stringValue, "on ramp")
    }

    func testManeuverTypeOffRamp() {
        XCTAssertEqual(ManeuverType.offRamp.stringValue, "off ramp")
    }

    func testManeuverTypeFork() {
        XCTAssertEqual(ManeuverType.fork.stringValue, "fork")
    }

    func testManeuverTypeEndOrRoad() {
        XCTAssertEqual(ManeuverType.endOfRoad.stringValue, "end of road")
    }

    func testManeuverTypeContinue() {
        XCTAssertEqual(ManeuverType.continue.stringValue, "continue")
    }

    func testManeuverTypeRoundabout() {
        XCTAssertEqual(ManeuverType.roundabout.stringValue, "roundabout")
    }

    func testManeuverTypeRotary() {
        XCTAssertEqual(ManeuverType.rotary.stringValue, "rotary")
    }

    func testManeuverTypeRoundaboutTurn() {
        XCTAssertEqual(ManeuverType.roundaboutTurn.stringValue, "roundabout turn")
    }

    func testManeuverTypeNotification() {
        XCTAssertEqual(ManeuverType.notification.stringValue, "notification")
    }

    func testManeuverTypeExitRoundabout() {
        XCTAssertEqual(ManeuverType.exitRoundabout.stringValue, "exit roundabout")
    }

    func testManeuverTypeExitRotary() {
        XCTAssertEqual(ManeuverType.exitRotary.stringValue, "exit rotary")
    }
}
