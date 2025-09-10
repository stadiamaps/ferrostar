import CarPlay
import FerrostarCore
import Foundation
import MapKit

public extension NavigationState {
    func updateEstimates(mapTemplate: CPMapTemplate, session: CPNavigationSession, units: MKDistanceFormatter.Units) {
        if let progress = currentProgress {
            progress.updateUpcomingEstimates(
                session: session,
                mapTemplate: mapTemplate,
                units: units
            )
        }

        if let instruction = currentVisualInstruction, let step = currentStep {
            session.updateEstimates(instruction: instruction, step: step, units: units)
        }
    }
}
