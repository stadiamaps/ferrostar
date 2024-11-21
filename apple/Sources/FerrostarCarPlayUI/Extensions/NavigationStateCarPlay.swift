import Foundation
import FerrostarCore
import CarPlay

extension NavigationState {
    
    var currentTravelEstimate: CPTravelEstimates? {
        guard let metersRemaining = self.currentProgress?.distanceRemaining,
              let secondsRemaining = self.currentProgress?.durationRemaining else { return nil }
        
        let distanceRemaining = Measurement(value: metersRemaining, unit: UnitLength.meters)
        
        return CPTravelEstimates(distanceRemaining: distanceRemaining,
                                 distanceRemainingToDisplay: distanceRemaining,
                                 timeRemaining: secondsRemaining)
    }
}
