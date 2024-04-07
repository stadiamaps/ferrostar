import SwiftUI

public protocol InstructionRowTheme {
    /// The color for the step distance (or distance to step).
    var distanceColor: Color { get }

    /// The font for the step distance (or distance to step).
    var distanceFont: Font { get }

    /// The color for instruction text.
    var instructionColor: Color { get }

    /// The font for the instruction text.
    var instructionFont: Font { get }

    /// The color of the icon.
    var iconTintColor: Color { get }

    /// The color of the background.
    var backgroundColor: Color { get }
}

public struct DefaultInstructionRowTheme: InstructionRowTheme, Equatable, Hashable {
    public var distanceColor: Color = .primary
    public var distanceFont: Font = .title.bold()
    public var instructionColor: Color = .secondary
    public var instructionFont: Font = .title2
    public var iconTintColor: Color = .primary
    public var backgroundColor: Color = .init(.systemBackground)

    public init() {
        // No action. Create your own theme or modify this inline if you want to customize
    }
}

public struct DefaultSecondaryInstructionRowTheme: InstructionRowTheme, Equatable, Hashable {
    public var distanceColor: Color = .primary
    public var distanceFont: Font = .title3.bold()
    public var instructionColor: Color = .secondary
    public var instructionFont: Font = .subheadline
    public var iconTintColor: Color = .primary
    public var backgroundColor: Color = .init(.secondarySystemBackground)

    public init() {
        // No action. Create your own theme or modify this inline if you want to customize
    }
}
