import FerrostarSwiftUI
import Foundation
import MapKit

/// A more restricted formatter collection for testing consistency.
class TestingFormatterCollection: FormatterCollection {
    var distanceFormatter: Formatter {
        _distanceFormatter
    }

    private var _distanceFormatter: MKDistanceFormatter = {
        let formatter = MKDistanceFormatter()
        formatter.unitStyle = .abbreviated
        formatter.locale = Locale(identifier: "en_US")
        return formatter
    }()

    var estimatedArrivalFormatter: Date.FormatStyle = .init(date: .omitted, time: .shortened)

    var durationFormatter: DateComponentsFormatter = {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = [.hour, .minute]
        formatter.unitsStyle = .abbreviated
        formatter.zeroFormattingBehavior = .dropAll
        return formatter
    }()

    var speedValueFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.maximumFractionDigits = 0
        formatter.locale = Locale(identifier: "en_US")
        return formatter
    }()

    var speedWithUnitsFormatter: MeasurementFormatter = {
        let formatter = MeasurementFormatter()
        formatter.unitOptions = .naturalScale
        formatter.unitStyle = .short
        formatter.locale = Locale(identifier: "en_US")
        return formatter
    }()

    /// Modify the locale of all formatters that support a locale.
    func locale(_ locale: Locale) -> Self {
        _distanceFormatter.locale = locale
        speedValueFormatter.locale = locale
        speedWithUnitsFormatter.locale = locale
        return self
    }
}
