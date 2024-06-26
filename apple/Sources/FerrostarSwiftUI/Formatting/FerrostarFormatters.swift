import Foundation

/// A group of formatters to apply to the UI stack.
///
/// This allows easily controlling the formatters throughout the UI. If fine grained
/// control over specific views is required, it's best to modify these directly using
/// the formatter inputs and your own UI wrappers.
public protocol FerrostarFormatters: AnyObject {
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

/// A default formatter collection.
///
/// This formatter combines our default formatters for application in the UI stack. Typical formatters
/// are very limited in configuration to accept the default iOS system behaviors for locale, etc. This can be customized
/// or you can create your own FerrostarFormatters definition to apply to the UI stack.
public class DefaultFerrostarFormatters: FerrostarFormatters {
    public var distanceFormatter: Formatter
    public var estimatedArrivalFormatter: Date.FormatStyle
    public var durationFormatter: DateComponentsFormatter
    public var speedValueFormatter: NumberFormatter
    public var speedWithUnitsFormatter: MeasurementFormatter

    public init(
        distanceFormatter: Formatter = DefaultFormatters.distanceFormatter,
        estimatedArrivalFormatter: Date.FormatStyle = DefaultFormatters.estimatedArrivalFormat,
        durationFormatter: DateComponentsFormatter = DefaultFormatters.durationFormat,
        speedValueFormatter: NumberFormatter = DefaultFormatters.speedFormatter,
        speedWithUnitsFormatter: MeasurementFormatter = DefaultFormatters.speedWithUnitsFormatter
    ) {
        self.distanceFormatter = distanceFormatter
        self.estimatedArrivalFormatter = estimatedArrivalFormatter
        self.durationFormatter = durationFormatter
        self.speedValueFormatter = speedValueFormatter
        self.speedWithUnitsFormatter = speedWithUnitsFormatter
    }
}
