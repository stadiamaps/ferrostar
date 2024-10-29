import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class MuteUIButtonTests: XCTestCase {
    func test_muted() {
        assertView {
            MuteUIButton(isMuted: true, action: {})
        }
    }

    func test_unmuted() {
        assertView {
            MuteUIButton(isMuted: false, action: {})
        }
    }

    // MARK: Dark Mode

    func test_muted_darkMode() {
        assertView(colorScheme: .dark) {
            MuteUIButton(isMuted: true, action: {})
        }
    }

    func test_unmuted_darkMode() {
        assertView(colorScheme: .dark) {
            MuteUIButton(isMuted: false, action: {})
        }
    }
}
