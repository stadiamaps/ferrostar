import CarPlay
import FerrostarCoreFFI
import Foundation

extension CPTravelEstimates {
    /// Create an trip travel estimate. This is used to update
    /// the bottom arrival view on the CarPlay screen.
    ///
    /// - Parameters:
    ///   - progress: The ferrostar trip progress.
    ///   - units: The desired unit style.
    ///   - locale: The locale, if using `.default` for units.
    /// - Returns: A car play travel estimate that can be applied to a CPMapTemplate
    static func fromFerrostarForTrip(
        progress: TripProgress,
        units: MKDistanceFormatter.Units,
        locale: Locale
    ) -> CPTravelEstimates {
        let distance = CarPlayMeasurementLength(
            units: units,
            distance: progress.distanceRemaining,
            locale: locale
        )

        // Convert duration remaining from seconds to TimeInterval
        let timeRemaining = TimeInterval(progress.durationRemaining)

        return CPTravelEstimates(
            distanceRemaining: distance.rounded(),
            timeRemaining: timeRemaining
        )
    }

    /// Create a step travel estimate. This is used to update
    /// the instructions bar.
    ///
    /// The distance and time are to the next maneuver, not to the destination.
    ///
    /// - Parameters:
    ///   - progress: The ferrostar trip progress.
    ///   - units: The desired unit style.
    ///   - locale: The locale, if using `.default` for units.
    /// - Returns: A car play travel estimate that can be applied to a CPMapTemplate
    static func fromFerrostarForStep(
        progress: TripProgress,
        units: MKDistanceFormatter.Units,
        locale: Locale
    ) -> CPTravelEstimates {
        let distance = CarPlayMeasurementLength(
            units: units,
            distance: progress.distanceToNextManeuver,
            locale: locale
        )

        // The core should not produce a negative or non-finite duration.
        // If it does, use zero so that CarPlay never gets an invalid time.
        let duration = progress.durationToNextManeuver
        let timeRemaining = duration.isFinite ? max(0, duration) : 0

        return CPTravelEstimates(
            distanceRemaining: distance.rounded(),
            timeRemaining: TimeInterval(timeRemaining)
        )
    }
}
