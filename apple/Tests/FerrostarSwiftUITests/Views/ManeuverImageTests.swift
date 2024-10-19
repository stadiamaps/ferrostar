import SwiftUI
import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ManeuverImageTests: XCTestCase {
    func testManeuverImageDefaultTheme() {
        assertView {
            ManeuverImage(maneuverType: .turn, maneuverModifier: .right)
                .frame(width: 128, height: 128)
        }

        assertView {
            ManeuverImage(maneuverType: .fork, maneuverModifier: .left)
                .frame(width: 32)
        }

        assertView {
            ManeuverImage(maneuverType: .turn, maneuverModifier: .uTurn)
                .frame(width: 32)
        }

        assertView {
            ManeuverImage(maneuverType: .continue, maneuverModifier: .uTurn)
                .frame(width: 32)
        }
    }

    func testManeuverImageLarge() {
        assertView {
            ManeuverImage(maneuverType: .rotary, maneuverModifier: .slightRight)
        }
    }

    func testManeuverImageCustomColor() {
        assertView {
            ManeuverImage(maneuverType: .merge, maneuverModifier: .slightLeft)
                .frame(width: 92)
                .foregroundColor(.blue)
        }
    }

    func testManeuverImageDoesNotExist() {
        assertView {
            ManeuverImage(maneuverType: .arrive, maneuverModifier: .slightLeft)
                .frame(width: 92)
                .foregroundColor(.white)
                .background(Color.green)
        }
    }
}
