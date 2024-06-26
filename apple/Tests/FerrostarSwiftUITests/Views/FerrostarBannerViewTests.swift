import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class FerrostarBannerViewTests: XCTestCase {
    func testInfoBanner() {
        assertView {
            FerrostarBanner(severity: .info) {
                Text("Something Useful")
            }
        }
    }

    func testLoadingBanner() {
        assertView {
            FerrostarBanner(severity: .loading) {
                Text("Rerouting...")
            }
        }
    }

    func testErrorBanner() {
        assertView {
            FerrostarBanner(severity: .error) {
                Text("No Location Available")
            }
        }
    }

    // MARK: Dark Mode

    func testInfoBanner_darkMode() {
        assertView(colorScheme: .dark) {
            FerrostarBanner(severity: .info) {
                Text("Something Useful")
            }
        }
    }

    func testLoadingBanner_darkMode() {
        assertView(colorScheme: .dark) {
            FerrostarBanner(severity: .loading) {
                Text("Rerouting...")
            }
        }
    }

    func testErrorBanner_darkMode() {
        assertView(colorScheme: .dark) {
            FerrostarBanner(severity: .error) {
                Text("No Location Available")
            }
        }
    }
}
