import CarPlay
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI

class NavigatingTemplateHost {
    private let mapTemplate: CPMapTemplate
    private let formatters: FormatterCollection
    private let units: MKDistanceFormatter.Units

    private var currentTrip: CPTrip?
    private var currentSession: CPNavigationSession?

    init(
        mapTemplate: CPMapTemplate,
        formatters: FormatterCollection,
        units: MKDistanceFormatter.Units
    ) {
        self.mapTemplate = mapTemplate
        self.formatters = formatters
        self.units = units
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func start(routes: [Route], waypoints: [Waypoint]) throws {
        let currentTrip: CPTrip = try .fromFerrostar(
            routes: routes,
            waypoints: waypoints,
            distanceFormatter: formatters.distanceFormatter,
            durationFormatter: formatters.durationFormatter
        )

        let currentSession = mapTemplate.startNavigationSession(for: currentTrip)

        self.currentTrip = currentTrip
        self.currentSession = currentSession
    }

    func update(navigationState: NavigationState) {
        updateArrival(navigationState.currentProgress)
    }

    func update(_ instruction: VisualInstruction, currentStep: RouteStep) {
        let stepDistance = CarPlayMeasurementLength(units: units, distance: currentStep.distance)

        let maneuvers = [
            CPManeuver.fromFerrostar(
                instruction,
                stepDuration: currentStep.duration,
                stepDistance: stepDistance.rounded()
            ),
        ].compactMap { $0 }

        currentSession?.upcomingManeuvers = maneuvers
    }

    private func updateArrival(_ progress: TripProgress?) {
        guard let currentTrip, let progress else {
            // TODO: Remove Progress?
            return
        }

        let estimates = CPTravelEstimates.fromFerrostarForTrip(
            progress: progress,
            units: units,
            locale: .current
        )

        mapTemplate.updateEstimates(estimates, for: currentTrip)

        if let currentManeuer = currentSession?.upcomingManeuvers.first {
            let estimates = CPTravelEstimates.fromFerrostarForStep(
                progress: progress,
                units: units,
                locale: .current
            )

            currentSession?.updateEstimates(estimates, for: currentManeuer)
        }
    }
}
