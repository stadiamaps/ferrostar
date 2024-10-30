import SwiftUI
import XCTest
@testable import FerrostarCore
@testable import FerrostarSwiftUI

final class NavigatingInnerGridViewTests: XCTestCase {
    func test_USStyle_speedLimit_inGridView() {
        assertView {
            NavigatingInnerGridView(
                speedLimit: .init(value: 55, unit: .milesPerHour),
                speedLimitStyle: .mutcdStyle,
                isMuted: true,
                onMute: {},
                showZoom: true,
                showCentering: true
            )
            .padding()
        }
    }

    func test_ViennaConventionStyle_speedLimit_inGridView() {
        assertView {
            NavigatingInnerGridView(
                speedLimit: .init(value: 100, unit: .kilometersPerHour),
                speedLimitStyle: .viennaConvention,
                isMuted: false,
                onMute: {},
                showZoom: true,
                showCentering: true
            )
            .environment(\.locale, .init(identifier: "fr_FR"))
            .padding()
        }
    }

    func test_muteIsHidden() {
        assertView {
            NavigatingInnerGridView(
                isMuted: true,
                showMute: false,
                onMute: {},
                showZoom: true,
                showCentering: true
            )
            .environment(\.locale, .init(identifier: "fr_FR"))
            .padding()
        }
    }
}
