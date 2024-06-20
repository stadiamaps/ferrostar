import Foundation

public protocol FerrostarTheme {
    var primaryInstructionsRow: InstructionRowTheme { get }
    var secondaryInstructionsRow: InstructionRowTheme { get }
    var arrival: any ArrivalViewTheme { get }

    // MARK: Formatters

    /// The core distance formatter. This is used in views like the instructions banner and arrival view.
    var distanceFormatter: Formatter { get }

    /// The estimated arrival time formatter. This is used by the arrival view.
    var estimatedArrivalFormatter: Date.FormatStyle { get }

    /// The duration formatter. This is used by the arrival view.
    var durationFormatter: DateComponentsFormatter { get }

    /// The speed value formatter is a number formatter that simply formats decimals
    /// on the speed value.
    var speedValueFormatter: NumberFormatter { get }

    /// The speed with units formatter is a measurement formatter used to format speed units for items like
    /// the user's current speed, speed limit and other related measurements.
    ///
    /// In the case of speed, it can be used on both `Measurement<UnitSpeed>` and `UnitSpeed` directly.
    var speedWithUnitsFormatter: MeasurementFormatter { get }
}

public struct DefaultFerrostarTheme: FerrostarTheme {
    // MARK: Themeing (Font, Color, etc)

    public var primaryInstructionsRow: any InstructionRowTheme
    public var secondaryInstructionsRow: any InstructionRowTheme
    public var arrival: any ArrivalViewTheme

    // MARK: Formatters

    public var distanceFormatter: Formatter
    public var estimatedArrivalFormatter: Date.FormatStyle
    public var durationFormatter: DateComponentsFormatter
    public var speedValueFormatter: NumberFormatter
    public var speedWithUnitsFormatter: MeasurementFormatter

    public init(
        primaryInstructionsRow: any InstructionRowTheme = DefaultInstructionRowTheme(),
        secondaryInstructionsRow: any InstructionRowTheme = DefaultSecondaryInstructionRowTheme(),
        arrival: any ArrivalViewTheme = DefaultArrivalViewTheme(),
        distanceFormatter: Formatter = DefaultFormatters.distanceFormatter,
        estimatedArrivalFormatter: Date.FormatStyle = DefaultFormatters.estimatedArrivalFormat,
        durationFormatter: DateComponentsFormatter = DefaultFormatters.durationFormat,
        speedValueFormatter: NumberFormatter = DefaultFormatters.speedFormatter,
        speedWithUnitsFormatter: MeasurementFormatter = DefaultFormatters.speedWithUnitsFormatter
    ) {
        self.primaryInstructionsRow = primaryInstructionsRow
        self.secondaryInstructionsRow = secondaryInstructionsRow
        self.arrival = arrival
        self.distanceFormatter = distanceFormatter
        self.estimatedArrivalFormatter = estimatedArrivalFormatter
        self.durationFormatter = durationFormatter
        self.speedValueFormatter = speedValueFormatter
        self.speedWithUnitsFormatter = speedWithUnitsFormatter
    }
}
