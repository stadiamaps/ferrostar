import XCTest
@testable import FerrostarSwiftUI

final class ArrivalViewThemeTests: XCTestCase {
    func testDefaultArrivalViewTheme() throws {
        let theme = DefaultArrivalViewTheme()

        XCTAssertEqual(theme.style, .full)
        XCTAssertEqual(theme.measurementColor, .primary)
        XCTAssertEqual(theme.measurementFont, .title2.bold())
        XCTAssertEqual(theme.secondaryColor, .secondary)
        XCTAssertEqual(theme.secondaryFont, .subheadline)
        XCTAssertEqual(theme.backgroundColor, .init(.systemBackground))
    }
}
