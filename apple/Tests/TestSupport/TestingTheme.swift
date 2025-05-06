import FerrostarSwiftUI
import Foundation
import SwiftUI

public struct TestingInstructionRowTheme: InstructionRowTheme {
    public init() {
        // No def
    }

    public let distanceColor: Color = .black
    public let distanceFont: Font = .title
    public let instructionColor: Color = .black
    public let instructionFont: Font = .title3
    public let iconTintColor: Color = .black
    public let backgroundColor: Color = .white
}
