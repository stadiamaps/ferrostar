import Foundation
import MapKit

public class ArrivalFormatters {
    
    public static var distanceFormatter: MKDistanceFormatter {
        let formatter = MKDistanceFormatter()
        formatter.unitStyle = .abbreviated
        return formatter
    }
    
    /// A standard formatter for estimated time of arrival.
    /// E.g. `10:21 AM`
    public static var estimatedArrivalFormat: Date.FormatStyle {
        Date.FormatStyle()
            .hour(.defaultDigits(amPM: .abbreviated))
            .minute(.twoDigits)
    }
    
    /// <#Description#>
    public static var durationFormat: DateComponentsFormatter {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = [.hour, .minute]
        formatter.unitsStyle = .abbreviated
        formatter.zeroFormattingBehavior = .dropAll
        return formatter
    }
}
