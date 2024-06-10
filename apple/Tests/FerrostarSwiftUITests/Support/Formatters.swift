import Foundation
import MapKit

var americanDistanceFormatter: Formatter = {
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial

    return formatter
}()

var germanDistanceFormatter: Formatter = {
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "de-DE")
    formatter.units = .metric

    return formatter
}()

var longDurationFormatter: DateComponentsFormatter = {
    let formatter = DateComponentsFormatter()
    formatter.unitsStyle = .full
    
    return formatter
}()
