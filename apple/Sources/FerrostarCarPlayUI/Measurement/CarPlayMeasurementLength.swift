import Foundation
import MapKit

struct CarPlayMeasurementLength {
    let measurement: Measurement<UnitLength>

    init(
        units: MKDistanceFormatter.Units,
        distance: CLLocationDistance,
        locale: Locale = .current
    ) {
        let meters = Measurement(value: distance, unit: UnitLength.meters)

        let (shortDistance, longDistance) = units.getShortAndLong(for: locale)
        let useShortDistance = distance <= units.thresholdForLargeUnit(for: locale)

        let desiredUnits = useShortDistance ? shortDistance : longDistance

        measurement = meters.converted(to: desiredUnits)
    }

    func rounded() -> Measurement<UnitLength> {
        let value = measurement.value

        switch measurement.unit {
        case .feet, .meters, .yards:
            if value < 50 {
                // Less than 50, round to nearest 5
                return Measurement(value: value.rounded(toNearest: 5), unit: .feet)
            } else if value < 100 {
                // Between 50 and 100, round to nearest 10
                return Measurement(value: value.rounded(toNearest: 10), unit: .feet)
            } else if value < 500 {
                // Between 100 and 500, round to nearest 50
                return Measurement(value: value.rounded(toNearest: 50), unit: .feet)
            } else {
                // Above 500, round to nearest 100
                return Measurement(value: value.rounded(toNearest: 100), unit: .feet)
            }

        default:
            // For kilometers and miles
            if value > 10 {
                // Round to nearest integer
                return Measurement(value: value.rounded(), unit: measurement.unit)
            } else {
                // Round to nearest 0.1
                return Measurement(value: (value * 10).rounded() / 10, unit: measurement.unit)
            }
        }
    }
}

private extension Double {
    func rounded(toNearest: Double) -> Double {
        (self / toNearest).rounded() * toNearest
    }
}
