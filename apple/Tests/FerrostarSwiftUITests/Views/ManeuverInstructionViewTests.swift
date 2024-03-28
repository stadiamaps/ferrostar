import SwiftUI
import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ManeuverInstructionViewTests: XCTestCase {
    func testCustomManeuverInstructionIcon() throws {
        assertView {
            ManeuverInstructionView(
                text: "Turn Right on Road Ave.",
                distanceToNextManeuver: 24140.16,
                theme: DefaultInstructionRowTheme()
            ) {
                Image(systemName: "car.circle.fill")
                    .symbolRenderingMode(.multicolor)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 32)
            }
        }
    }

    func testFerrostarInstruction() throws {
        assertView {
            ManeuverInstructionView(
                text: "Merge Left",
                distanceToNextManeuver: 152.4
            ) {
                ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                    .frame(width: 24)
            }
            .font(.body)
            .foregroundColor(.blue)
        }
    }

    func testRightToLeftInstruction() throws {
        assertView {
            ManeuverInstructionView(
                text: "ادمج يسارًا"
            ) {
                ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                    .frame(width: 24)
            }
            .environment(\.layoutDirection, .rightToLeft)
        }
    }
}
