import Foundation

/// A group of formatters to apply to the UI stack.
///
/// This is set in the SwiftUI environment to enable easy control across the whole UI. If you need fine-grained
/// control over specific views, it's best to modify these directly using
/// the formatter inputs and your own UI wrappers.
public protocol FormatterCollection: AnyObject {
    /// The core distance formatter. This is used in views like the instructions banner and trip progress view.
    var distanceFormatter: Formatter { get }

    /// The estimated arrival time formatter. This is used by the trip progress view.
    var estimatedArrivalFormatter: Date.FormatStyle { get }

    /// The duration formatter. This is used by the trip progress view.
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

/// An adaptable collection of `Foundation` formatters.
///
/// The default constructor uses formatters from ``DefaultFormatters``, which is what most applications want,
/// as this automatically reflects user locale preferences.
/// If you want to change any of the behaviors but still use `Foundation` formatters, you can pass a preconfigured
/// formatter for any parameter.
public class FoundationFormatterCollection: FormatterCollection {
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
