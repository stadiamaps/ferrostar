import Foundation

public protocol NavigationUITheme: AnyObject {
    var primaryInstructionsRow: InstructionRowTheme { get }
    var secondaryInstructionsRow: InstructionRowTheme { get }
    var tripProgress: any TripProgressViewTheme { get }
}

public class DefaultNavigationUITheme: NavigationUITheme {
    public var primaryInstructionsRow: any InstructionRowTheme
    public var secondaryInstructionsRow: any InstructionRowTheme
    public var tripProgress: any TripProgressViewTheme

    public init(
        primaryInstructionsRow: any InstructionRowTheme = DefaultInstructionRowTheme(),
        secondaryInstructionsRow: any InstructionRowTheme = DefaultSecondaryInstructionRowTheme(),
        tripProgress: any TripProgressViewTheme = DefaultTripProgressViewTheme()
    ) {
        self.primaryInstructionsRow = primaryInstructionsRow
        self.secondaryInstructionsRow = secondaryInstructionsRow
        self.tripProgress = tripProgress
    }
}
