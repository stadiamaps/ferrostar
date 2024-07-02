import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class NavigationUIButtonTests: XCTestCase {
    func testImageButton() {
        assertView {
            NavigationUIButton {} label: {
                Image(systemName: "location")
            }
        }
    }

    func testTextButton() {
        assertView {
            NavigationUIButton {} label: {
                Text("Start Navigation")
            }
        }
    }

    // MARK: Dark Mode

    func testImageButton_darkMode() {
        assertView(colorScheme: .dark) {
            NavigationUIButton {} label: {
                Image(systemName: "location")
            }
        }
    }

    func testTextButton_darkMode() {
        assertView(colorScheme: .dark) {
            NavigationUIButton {} label: {
                Text("Start Navigation")
            }
        }
    }
}
