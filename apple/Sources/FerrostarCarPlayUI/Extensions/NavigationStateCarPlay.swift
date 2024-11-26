import CarPlay
import FerrostarCore
import Foundation

extension NavigationState {
    var currentTravelEstimate: CPTravelEstimates? {
        guard let metersRemaining = currentProgress?.distanceRemaining,
              let secondsRemaining = currentProgress?.durationRemaining else { return nil }

        let distanceRemaining = Measurement(value: metersRemaining, unit: UnitLength.meters)

        return CPTravelEstimates(distanceRemaining: distanceRemaining,
                                 distanceRemainingToDisplay: distanceRemaining,
                                 timeRemaining: secondsRemaining)
    }
}
