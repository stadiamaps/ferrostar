import CarPlay
import FerrostarCoreFFI

extension CPNavigationSession {
    func updateEstimates(instruction: VisualInstruction, step: RouteStep, units: MKDistanceFormatter.Units) {
        let currentManeuver = upcomingManeuvers.first
        let drivingSide = step.drivingSide ?? .right
        if currentManeuver?.visualInstruction != instruction || currentManeuver?.drivingSide != drivingSide {
            let stepDistance = CarPlayMeasurementLength(units: units, distance: step.distance)

            upcomingManeuvers = [instruction.maneuver(
                stepDuration: step.duration,
                stepDistance: stepDistance.rounded(),
                drivingSide: drivingSide
            )]
        }
    }
}
