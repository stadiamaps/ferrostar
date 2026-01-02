import CarPlay
import FerrostarCoreFFI
import Foundation

extension TripProgress {
    func updateUpcomingEstimates(
        session: CPNavigationSession,
        mapTemplate: CPMapTemplate,
        units: MKDistanceFormatter.Units
    ) {
        let estimates = CPTravelEstimates.fromFerrostarForTrip(
            progress: self,
            units: units,
            locale: .current
        )

        mapTemplate.updateEstimates(estimates, for: session.trip)

        if let currentManeuver = session.upcomingManeuvers.first {
            let estimates = CPTravelEstimates.fromFerrostarForStep(
                progress: self,
                units: units,
                locale: .current
            )

            session.updateEstimates(estimates, for: currentManeuver)
        }
    }
}
