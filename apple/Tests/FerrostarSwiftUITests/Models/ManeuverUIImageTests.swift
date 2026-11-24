import FerrostarCoreFFI
import FerrostarSwiftUI
import UIKit
import XCTest

final class ManeuverUIImageTests: XCTestCase {
    func testLeftDrivingRoundaboutAssetsLoadWithReflectedOrientation() throws {
        let modifiers: [ManeuverModifier?] = [
            nil, .sharpLeft, .left, .slightLeft, .straight, .slightRight, .right, .sharpRight,
        ]

        for type in [ManeuverType.roundabout, .rotary] {
            for modifier in modifiers {
                let image = try XCTUnwrap(ManeuverUIImage(maneuverType: type,
                                                          maneuverModifier: modifier,
                                                          drivingSide: .left).uiImage)
                XCTAssertEqual(image.imageOrientation, .upMirrored)
            }
        }
    }

    func testUTurnAssetsLoadForBothDrivingSides() throws {
        for type in [ManeuverType.turn, .continue] {
            let left = try XCTUnwrap(ManeuverUIImage(maneuverType: type,
                                                     maneuverModifier: .uTurn,
                                                     drivingSide: .left).uiImage)
            let right = try XCTUnwrap(ManeuverUIImage(maneuverType: type,
                                                      maneuverModifier: .uTurn,
                                                      drivingSide: .right).uiImage)

            XCTAssertEqual(left.imageOrientation, .upMirrored)
            XCTAssertEqual(right.imageOrientation, .up)
        }
    }

    func testOrdinaryTurnRetainsImageOrientation() throws {
        for side in [DrivingSide.left, .right] {
            let image = try XCTUnwrap(ManeuverUIImage(maneuverType: .turn,
                                                      maneuverModifier: .left,
                                                      drivingSide: side).uiImage)
            XCTAssertEqual(image.imageOrientation, .up)
        }
    }
}
