import Foundation
import MapKit

/// A collection of arrival view formatters that work reasonably well for most applications.
public class DefaultFormatters {
    /// An MKDistance formatter with abbreviated units for the arrival view.
    ///
    /// E.g. 120 mi
    public static var distanceFormatter: MKDistanceFormatter {
        let formatter = MKDistanceFormatter()
        formatter.unitStyle = .abbreviated
        return formatter
    }

    /// A formatter for estimated time of arrival using the shortened style for the current locale.
    ///
    /// E.g. `5:20 PM` or `17:20`
    public static var estimatedArrivalFormat: Date.FormatStyle {
        Date.FormatStyle(date: .omitted, time: .shortened)
    }

    /// A formatter for duration on the arrival view using (optional) hours and minutes.
    ///
    /// E.g. `1h 20m`
    public static var durationFormat: DateComponentsFormatter {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = [.hour, .minute]
        formatter.unitsStyle = .abbreviated
        formatter.zeroFormattingBehavior = .dropAll
        return formatter
    }

    // MARK: Speed

    /// The speed with units formatter is a measurement formatter used to format speed units for items like
    /// the user's current speed, speed limit and other related measurements.
    ///
    /// E.g. `50 mi/hr` or `100 km/hr`
    public static var speedWithUnitsFormatter: MeasurementFormatter {
        let formatter = MeasurementFormatter()
        formatter.unitOptions = .naturalScale
        formatter.unitStyle = .short
        return formatter
    }

    /// A standard formatter for speed numbers without any decimal digits (e.g. for a speed limit)
    ///
    /// E.g. `50`
    public static var speedFormatter: NumberFormatter {
        let formatter = NumberFormatter()
        formatter.maximumFractionDigits = 0
        return formatter
    }
}
