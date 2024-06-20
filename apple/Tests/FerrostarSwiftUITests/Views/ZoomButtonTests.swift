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
}
