import XCTest
@testable import FerrostarSwiftUI

final class NavigationUIZoomButtonTests: XCTestCase {
    func testNavigationUIZoomButton() {
        assertView {
            NavigationUIZoomButton(
                onZoomIn: {},
                onZoomOut: {}
            )
        }
    }

    // MARK: Dark Mode

    func testNavigationUIZoomButton_darkMode() {
        assertView(colorScheme: .dark) {
            NavigationUIZoomButton(
                onZoomIn: {},
                onZoomOut: {}
            )
        }
    }
}
