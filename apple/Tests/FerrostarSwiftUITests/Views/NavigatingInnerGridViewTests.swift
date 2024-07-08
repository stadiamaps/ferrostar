import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class NavigatingInnerGridViewTests: XCTestCase {
    // TODO: enable once we decide on a method to expose the speed limit sign provider within the view stack.
//    func testUSView() {
//        assertView {
//            NavigatingInnerGridView(
//                speedLimit: .init(value: 55, unit: .milesPerHour),
//                showZoom: true,
//                showCentering: true
//            )
//            .padding()
//        }
//    }

    func testViennaStyleSpeedLimitInGridView() {
        assertView {
            NavigatingInnerGridView(
                speedLimit: .init(value: 100, unit: .kilometersPerHour),
                showZoom: true,
                showCentering: true
            )
            .environment(\.locale, .init(identifier: "fr_FR"))
            .padding()
        }
    }
}
