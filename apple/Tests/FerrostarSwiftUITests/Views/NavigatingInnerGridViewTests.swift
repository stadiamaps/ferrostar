import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class NavigatingInnerGridViewTests: XCTestCase {

    func test_USStyle_speedLimit_inGridView() {
        assertView {
            NavigatingInnerGridView(
                speedLimit: .init(value: 55, unit: .milesPerHour),
                speedLimitStyle: .usStyle,
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
                showZoom: true,
                showCentering: true
            )
            .environment(\.locale, .init(identifier: "fr_FR"))
            .padding()
        }
    }
}
