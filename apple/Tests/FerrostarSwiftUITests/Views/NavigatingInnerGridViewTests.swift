import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class NavigatingInnerGridViewTests: XCTestCase {
    func testUSView() {
        assertView {
            NavigatingInnerGridView(
                speedLimit: .init(value: 55, unit: .milesPerHour),
                showZoom: true,
                showCentering: true
            )
            .padding()
        }
    }

    func testROWView() {
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
