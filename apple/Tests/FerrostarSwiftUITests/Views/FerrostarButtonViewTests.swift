import SwiftUI
import XCTest
@testable import FerrostarSwiftUI

final class FerrostarButtonViewTests: XCTestCase {
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
}
