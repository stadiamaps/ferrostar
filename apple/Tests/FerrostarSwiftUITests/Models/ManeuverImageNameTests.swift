import FerrostarCoreFFI
import FerrostarSwiftUI
import XCTest

final class ManeuverImageNameTests: XCTestCase {
    func testLeftDrivingRoundaboutsRetainTheirExitDirection() {
        let exits: [(ManeuverModifier?, String)] = [
            (nil, ""),
            (.sharpLeft, "_sharp_right"),
            (.left, "_right"),
            (.slightLeft, "_slight_right"),
            (.straight, "_straight"),
            (.slightRight, "_slight_left"),
            (.right, "_left"),
            (.sharpRight, "_sharp_left"),
        ]

        for (type, prefix) in [(ManeuverType.roundabout, "roundabout"), (.rotary, "rotary")] {
            for (modifier, reflectedExit) in exits {
                let name = ManeuverImageName(maneuverType: type,
                                             maneuverModifier: modifier,
                                             drivingSide: .left)

                XCTAssertEqual(name.value, prefix + reflectedExit)
                XCTAssertTrue(name.shouldMirror)
            }
        }
    }

    func testRightDrivingRoundaboutsKeepExistingAssets() {
        let exits: [(ManeuverModifier?, String)] = [
            (nil, ""),
            (.sharpLeft, "_sharp_left"),
            (.left, "_left"),
            (.slightLeft, "_slight_left"),
            (.straight, "_straight"),
            (.slightRight, "_slight_right"),
            (.right, "_right"),
            (.sharpRight, "_sharp_right"),
        ]

        for (type, prefix) in [(ManeuverType.roundabout, "roundabout"), (.rotary, "rotary")] {
            for (modifier, suffix) in exits {
                let name = ManeuverImageName(maneuverType: type,
                                             maneuverModifier: modifier,
                                             drivingSide: .right)

                XCTAssertEqual(name.value, prefix + suffix)
                XCTAssertFalse(name.shouldMirror)
            }
        }
    }

    func testBothUTurnTypesFollowTheDrivingSide() {
        for (type, asset) in [(ManeuverType.turn, "turn_uturn"), (.continue, "continue_uturn")] {
            for side in [DrivingSide.left, .right] {
                let name = ManeuverImageName(maneuverType: type,
                                             maneuverModifier: .uTurn,
                                             drivingSide: side)

                XCTAssertEqual(name.value, asset)
                XCTAssertEqual(name.shouldMirror, side == .left)
            }
        }
    }

    func testOrdinaryArrowsDoNotChangeWithDrivingSide() {
        let arrows: [(ManeuverType, ManeuverModifier, String)] = [
            (.turn, .sharpLeft, "turn_sharp_left"),
            (.turn, .left, "turn_left"),
            (.turn, .slightLeft, "turn_slight_left"),
            (.turn, .straight, "turn_straight"),
            (.turn, .slightRight, "turn_slight_right"),
            (.turn, .right, "turn_right"),
            (.turn, .sharpRight, "turn_sharp_right"),
            (.fork, .left, "fork_left"),
            (.merge, .right, "merge_right"),
            (.onRamp, .slightRight, "on_ramp_slight_right"),
            (.offRamp, .slightLeft, "off_ramp_slight_left"),
            (.endOfRoad, .left, "end_of_road_left"),
        ]

        for (type, modifier, asset) in arrows {
            for side in [DrivingSide.left, .right] {
                let name = ManeuverImageName(maneuverType: type,
                                             maneuverModifier: modifier,
                                             drivingSide: side)

                XCTAssertEqual(name.value, asset)
                XCTAssertFalse(name.shouldMirror)
            }
        }
    }

    func testOmittingDrivingSidePreservesRightDrivingDefault() {
        let roundabout = ManeuverImageName(maneuverType: .roundabout, maneuverModifier: .left)
        XCTAssertEqual(roundabout.value, "roundabout_left")
        XCTAssertFalse(roundabout.shouldMirror)

        let uTurn = ManeuverImageName(maneuverType: .turn, maneuverModifier: .uTurn)
        XCTAssertEqual(uTurn.value, "turn_uturn")
        XCTAssertFalse(uTurn.shouldMirror)
    }

    func testUnsupportedCombinationsRemainUnsupported() {
        for side in [DrivingSide.left, .right] {
            let arrive = ManeuverImageName(maneuverType: .arrive,
                                           maneuverModifier: .slightLeft,
                                           drivingSide: side)
            XCTAssertEqual(arrive.value, "arrive_slight_left")
            XCTAssertFalse(arrive.shouldMirror)

            let exit = ManeuverImageName(maneuverType: .exitRoundabout,
                                         maneuverModifier: .right,
                                         drivingSide: side)
            XCTAssertEqual(exit.value, "exit_roundabout_right")
            XCTAssertFalse(exit.shouldMirror)
        }
    }
}
