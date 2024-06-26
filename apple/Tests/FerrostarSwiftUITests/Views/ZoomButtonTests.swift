import XCTest
@testable import FerrostarSwiftUI

final class ZoomButtonTests: XCTestCase {
    func testZoomButton() {
        assertView {
            ZoomButton(
                onZoomIn: {},
                onZoomOut: {}
            )
        }
    }

    // MARK: Dark Mode

    func testZoomButton_darkMode() {
        assertView(colorScheme: .dark) {
            ZoomButton(
                onZoomIn: {},
                onZoomOut: {}
            )
        }
    }
}
