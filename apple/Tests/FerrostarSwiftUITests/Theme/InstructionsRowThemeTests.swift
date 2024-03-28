import XCTest
@testable import FerrostarSwiftUI

final class InstructionsRowThemeTests: XCTestCase {
    func testDefaultInstructionRowTheme() throws {
        let theme = DefaultInstructionRowTheme()

        XCTAssertEqual(theme.distanceColor, .primary)
        XCTAssertEqual(theme.distanceFont, .title.bold())
        XCTAssertEqual(theme.instructionColor, .secondary)
        XCTAssertEqual(theme.instructionFont, .title2)
        XCTAssertEqual(theme.iconTintColor, .primary)
    }

    func testDefaultSecondaryInstructionRowTheme() throws {
        let theme = DefaultSecondaryInstructionRowTheme()

        XCTAssertEqual(theme.distanceColor, .primary)
        XCTAssertEqual(theme.distanceFont, .title3.bold())
        XCTAssertEqual(theme.instructionColor, .secondary)
        XCTAssertEqual(theme.instructionFont, .subheadline)
        XCTAssertEqual(theme.iconTintColor, .primary)
    }
}
