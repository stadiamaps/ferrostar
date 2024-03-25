import SwiftUI

public protocol InstructionRowTheme {
    
    /// The color for primary instruction.
    var instructionColor: Color { get }
    
    /// The font for primary instruction.
    var instructionFont: Font { get }
    
    /// The color for the step distance (or distance to step).
    var distanceColor: Color { get }
    
    /// The font for the step distance (or distance to step).
    var distanceFont: Font { get }
    
    /// The color of the icon.
    var iconTintColor: Color { get }
    
    /// The color of the background.
    var backgroundColor: Color { get }
}

public struct DefaultInstructionRowTheme: InstructionRowTheme {
    public var instructionColor: Color = .primary
    public var instructionFont: Font = .title
    public var distanceColor: Color = .secondary
    public var distanceFont: Font = .subheadline
    public var iconTintColor: Color = .primary
    public var backgroundColor: Color = .white // TODO: Dynamic color
}
