import Foundation
import MapKit

/// A standard collection of arrival view formatters.
public class ArrivalFormatters {
    
    /// An MKDistance formatter with abbreviated units for the arrival view.
    ///
    /// E.g. 120 mi
    public static var distanceFormatter: MKDistanceFormatter {
        let formatter = MKDistanceFormatter()
        formatter.unitStyle = .abbreviated
        return formatter
    }
    
    /// A standard formatter for estimated time of arrival.
    ///
    /// E.g. `10:21 AM`
    public static var estimatedArrivalFormat: Date.FormatStyle {
        Date.FormatStyle()
            .hour(.defaultDigits(amPM: .abbreviated))
            .minute(.twoDigits)
    }
    
    /// The formatter for duration on the arrival view.
    public static var durationFormat: DateComponentsFormatter {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = [.hour, .minute]
        formatter.unitsStyle = .abbreviated
        formatter.zeroFormattingBehavior = .dropAll
        return formatter
    }
}
