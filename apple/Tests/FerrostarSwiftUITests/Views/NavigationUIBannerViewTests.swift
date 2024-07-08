import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class NavigationUIBannerViewTests: XCTestCase {
    func testInfoBanner() {
        assertView {
            NavigationUIBanner(severity: .info) {
                Text("Something Useful")
            }
        }
    }

    func testLoadingBanner() {
        assertView {
            NavigationUIBanner(severity: .loading) {
                Text("Rerouting...")
            }
        }
    }

    func testErrorBanner() {
        assertView {
            NavigationUIBanner(severity: .error) {
                Text("No Location Available")
            }
        }
    }

    // MARK: Dark Mode

    func testInfoBanner_darkMode() {
        assertView(colorScheme: .dark) {
            NavigationUIBanner(severity: .info) {
                Text("Something Useful")
            }
        }
    }

    func testLoadingBanner_darkMode() {
        assertView(colorScheme: .dark) {
            NavigationUIBanner(severity: .loading) {
                Text("Rerouting...")
            }
        }
    }

    func testErrorBanner_darkMode() {
        assertView(colorScheme: .dark) {
            NavigationUIBanner(severity: .error) {
                Text("No Location Available")
            }
        }
    }
}
