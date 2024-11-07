import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class MuteUIButtonTests: XCTestCase {
    func test_muted() {
        assertView {
            NavigationUIMuteButton(isMuted: true, action: {})
        }
    }

    func test_unmuted() {
        assertView {
            NavigationUIMuteButton(isMuted: false, action: {})
        }
    }

    // MARK: Dark Mode

    func test_muted_darkMode() {
        assertView(colorScheme: .dark) {
            NavigationUIMuteButton(isMuted: true, action: {})
        }
    }

    func test_unmuted_darkMode() {
        assertView(colorScheme: .dark) {
            NavigationUIMuteButton(isMuted: false, action: {})
        }
    }
}
