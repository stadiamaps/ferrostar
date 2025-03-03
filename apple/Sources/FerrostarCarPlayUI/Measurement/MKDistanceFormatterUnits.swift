import Foundation
import MapKit

extension MKDistanceFormatter.Units {
    /// Get the ideal short and long distance units.
    ///
    /// This mimicks the behavior inside the ``MKDistanceFormatter``, but returns
    /// both as ``UnitLength`` as required by CarPlay's MapTemplate/Session
    ///
    /// - Parameter locale: The locale to get prefered units for. This is usually `.current`
    /// - Returns: A tuple with the short distance and long distance units (e.g. feet and miles in order).
    func getShortAndLong(for locale: Locale) -> (UnitLength, UnitLength) {
        switch self {
        case .default:
            if locale.usesMetricSystem {
                return (.meters, .kilometers)
            } else {
                return locale.identifier == "en_GB" ?
                    (.yards, .miles) :
                    (.feet, .miles)
            }
        case .metric:
            return (.meters, .kilometers)
        case .imperial:
            return (.feet, .miles)
        case .imperialWithYards:
            return (.yards, .miles)
        @unknown default:
            return (.meters, .kilometers) // Default to metric
        }
    }

    /// Get the thresold for specifying a short or long unit in meters
    ///
    /// - Parameter locale: The locale to get prefered units for. This is usually `.current`
    /// - Returns: The distance below which the short unit is preferred, above which the long distance is preferred.
    func thresholdForLargeUnit(for locale: Locale) -> CLLocationDistance {
        switch self {
        case .default:
            if locale.usesMetricSystem {
                return 1000
            } else {
                return locale.identifier == "en_GB" ? 300 : 289
            }
        case .metric:
            return 1000
        case .imperial:
            return 289
        case .imperialWithYards:
            return 300
        @unknown default:
            return 1000
        }
    }
}
