import MapKit
import SwiftUI
import TestSupport
import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ManeuverInstructionViewTests: XCTestCase {
    func testCustomManeuverInstructionIcon() {
        assertView {
            ManeuverInstructionView(
                text: "Turn Right on Road Ave.",
                distanceFormatter: usaDistanceFormatter,
                distanceToNextManeuver: 24140.16,
                theme: TestingInstructionRowTheme()
            ) {
                Image(systemName: "car.circle.fill")
                    .symbolRenderingMode(.multicolor)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 32)
            }
            .background(.white)
        }
    }

    func testFerrostarInstruction() {
        assertView {
            ManeuverInstructionView(
                text: "Merge Left",
                distanceFormatter: usaDistanceFormatter,
                distanceToNextManeuver: 152.4,
                theme: TestingInstructionRowTheme()
            ) {
                ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                    .frame(width: 24)
            }
            .font(.body)
            .foregroundColor(.blue)
            .background(.white)
        }
    }

    func testFerrostarInstructionDE() {
        assertView {
            ManeuverInstructionView(
                text: "Links einfädeln",
                distanceFormatter: germanDistanceFormatter,
                distanceToNextManeuver: 152.4,
                theme: TestingInstructionRowTheme()
            ) {
                ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                    .frame(width: 24)
            }
            .font(.body)
            .foregroundColor(.blue)
            .background(.white)
        }
    }

    func testRightToLeftInstruction() {
        assertView {
            ManeuverInstructionView(
                text: "ادمج يسارًا",
                distanceFormatter: usaDistanceFormatter,
                theme: TestingInstructionRowTheme()
            ) {
                ManeuverImage(maneuverType: .merge, maneuverModifier: .left)
                    .frame(width: 24)
            }
            .environment(\.layoutDirection, .rightToLeft)
            .background(.white)
        }
    }
}
