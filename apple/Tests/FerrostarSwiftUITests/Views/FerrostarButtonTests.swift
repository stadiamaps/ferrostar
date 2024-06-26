import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class FerrostarButtonTests: XCTestCase {
    func testImageButton() {
        assertView {
            FerrostarButton {} label: {
                Image(systemName: "location")
            }
        }
    }

    func testTextButton() {
        assertView {
            FerrostarButton {} label: {
                Text("Start Navigation")
            }
        }
    }
    
    // MARK: Dark Mode
    
    func testImageButton_darkMode() {
        assertView(colorScheme: .dark)  {
            FerrostarButton {} label: {
                Image(systemName: "location")
            }
        }
    }

    func testTextButton_darkMode() {
        assertView(colorScheme: .dark) {
            FerrostarButton {} label: {
                Text("Start Navigation")
            }
        }
    }
}
