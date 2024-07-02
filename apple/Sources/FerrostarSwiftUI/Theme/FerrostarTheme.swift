import Foundation

public protocol NavigationUITheme: AnyObject {
    var primaryInstructionsRow: InstructionRowTheme { get }
    var secondaryInstructionsRow: InstructionRowTheme { get }
    var arrival: any ArrivalViewTheme { get }
}

public class DefaultNavigationUITheme: FerrostarTheme {
    public var primaryInstructionsRow: any InstructionRowTheme
    public var secondaryInstructionsRow: any InstructionRowTheme
    public var arrival: any ArrivalViewTheme

    public init(
        primaryInstructionsRow: any InstructionRowTheme = DefaultInstructionRowTheme(),
        secondaryInstructionsRow: any InstructionRowTheme = DefaultSecondaryInstructionRowTheme(),
        arrival: any ArrivalViewTheme = DefaultArrivalViewTheme()
    ) {
        self.primaryInstructionsRow = primaryInstructionsRow
        self.secondaryInstructionsRow = secondaryInstructionsRow
        self.arrival = arrival
    }
}
