import CarPlay
import FerrostarCoreFFI

extension CPNavigationSession {
    func updateEstimates(instruction: VisualInstruction, step: RouteStep, units: MKDistanceFormatter.Units) {
        let currentManeuver = upcomingManeuvers.first
        if currentManeuver == nil || (currentManeuver!.visualInstruction != instruction) {
            let stepDistance = CarPlayMeasurementLength(units: units, distance: step.distance)

            upcomingManeuvers = [instruction.maneuver(
                stepDuration: step.duration,
                stepDistance: stepDistance.rounded()
            )]
        }
    }
}
