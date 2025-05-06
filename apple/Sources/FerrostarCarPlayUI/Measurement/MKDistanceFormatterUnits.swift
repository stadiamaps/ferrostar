import Foundation
import MapKit

extension MKDistanceFormatter.Units {
    /// Get the ideal short and long distance units.
    ///
    /// This mimics the behavior inside the ``MKDistanceFormatter``, but returns
    /// both as ``UnitLength`` as required by CarPlay's MapTemplate/Session
    ///
    /// - Parameter locale: The locale to get preferred units for. This is usually `.current`
    /// - Returns: A tuple with the short distance and long distance units (e.g. feet and miles in order).
    func getShortAndLong(for locale: Locale) -> (UnitLength, UnitLength) {
        switch self {
        case .default:
            switch locale.measurementSystem {
            case .us:
                (.feet, .miles)
            case .uk:
                (.yards, .miles)
            default:
                (.meters, .kilometers)
            }
        case .imperial:
            (.feet, .miles)
        case .imperialWithYards:
            (.yards, .miles)
        default:
            (.meters, .kilometers) // Default to metric
        }
    }

    /// Get the threshold for specifying a short or long unit in meters
    ///
    /// - Parameter locale: The locale to get preferred units for. This is usually `.current`
    /// - Returns: The distance below which the short unit is preferred, above which the long distance is preferred.
    func thresholdForLargeUnit(for locale: Locale) -> CLLocationDistance {
        switch self {
        case .default:
            switch locale.measurementSystem {
            case .us:
                289
            case .uk:
                300
            default:
                1000
            }
        case .imperial:
            289
        case .imperialWithYards:
            300
        default:
            1000
        }
    }
}
