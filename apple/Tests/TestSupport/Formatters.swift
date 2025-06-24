import FerrostarSwiftUI
import Foundation
import MapKit

public var usaDistanceFormatter: Formatter = {
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "en-US")
    formatter.units = .imperial
    formatter.unitStyle = .abbreviated

    return formatter
}()

public var germanDistanceFormatter: Formatter = {
    let formatter = MKDistanceFormatter()
    formatter.locale = Locale(identifier: "de-DE")
    formatter.units = .metric
    formatter.unitStyle = .abbreviated

    return formatter
}()

public var longDurationFormatter: DateComponentsFormatter = {
    let formatter = DateComponentsFormatter()
    formatter.unitsStyle = .full

    return formatter
}()

public var germanArrivalFormatter: Date.FormatStyle = {
    var formatter = DefaultFormatters.estimatedArrivalFormat
        .locale(.init(identifier: "de_DE"))

    formatter.timeZone = .init(secondsFromGMT: 0)!

    return formatter
}()
