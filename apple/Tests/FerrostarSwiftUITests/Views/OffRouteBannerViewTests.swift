import TestSupport
import XCTest
@testable import FerrostarCore
@testable import FerrostarCoreFFI
@testable import FerrostarSwiftUI

final class OffRouteBannerViewTests: XCTestCase {
    func testOffRouteBannerView() {
        assertView {
            OffRouteBannerView().padding()
        }
    }

    func testOffRouteBannerViewCustomLargeMessage() {
        assertView {
            OffRouteBannerView(
                message: "You are off route, a re-route may be underway but you may also be able to return to the route if you have no data."
            ).padding()
        }
    }
}
