import MapKit
import SwiftUI
import TestSupport
import XCTest
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class ManeuverInstructionViewTests: XCTestCase {
    func testCustomManeuverInstructionIcon() throws {
        assertView {
            ManeuverInstructionView(
                text: "Turn Right on Road Ave.",
                distanceFormatter: americanDistanceFormatter,
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

    func testFerrostarInstruction() throws {
        assertView {
            ManeuverInstructionView(
                text: "Merge Left",
                distanceFormatter: americanDistanceFormatter,
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

    func testFerrostarInstructionDE() throws {
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

    func testRightToLeftInstruction() throws {
        assertView {
            ManeuverInstructionView(
                text: "ادمج يسارًا",
                distanceFormatter: americanDistanceFormatter,
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
