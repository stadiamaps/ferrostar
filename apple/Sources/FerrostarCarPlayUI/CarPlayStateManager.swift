import CarPlay
import Combine
import FerrostarCore

@MainActor
class CarPlayStateManager: CPNavigationSession, ObservableObject {
    @Published var currentManeuver: CPManeuver?
    @Published var travelEstimates: CPTravelEstimates?
    @Published var mapButtons: [CPMapButton]?

    private var ferrostarCore: FerrostarCore
    private var cancellables = Set<AnyCancellable>()
    private weak var interfaceController: CPInterfaceController?

    init(ferrostarCore: FerrostarCore) {
        self.ferrostarCore = ferrostarCore
        setupStateSync()
    }

    func startNavigation(
        with route: Route,
        using interface: CPInterfaceController,
        config: SwiftNavigationControllerConfig? = nil
    ) throws {
        interfaceController = interface

        // Start Ferrostar navigation
        try ferrostarCore.startNavigation(route: route, config: config)

        // Push this navigation session to CarPlay
        interface.pushNavigationSession(self)
    }

    func endNavigation() {
        interfaceController?.popNavigationSession(self)
        ferrostarCore.stopNavigation()
    }

    private func setupStateSync() {
        ferrostarCore.$state
            .sink { [weak self] navState in
                guard let self,
                      case let .navigating(
                          currentStepGeometryIndex: _,
                          snappedUserLocation: _,
                          remainingSteps: steps,
                          remainingWaypoints: _,
                          progress: progress,
                          deviation: _,
                          visualInstruction: instruction,
                          spokenInstruction: _,
                          annotationJson: _
                      ) = navState?.tripState
                else {
                    return
                }

                // Update current maneuver
                let maneuver = CPManeuver()
                if let instruction {
                    maneuver.instructionVariants = [instruction.primaryText]
                    // Set other maneuver properties based on instruction
                }
                currentManeuver = maneuver

                // Update travel estimates
                let estimates = CPTravelEstimates(
                    distanceRemaining: progress.distanceRemaining,
                    timeRemaining: TimeInterval(progress.durationRemaining)
                )
                travelEstimates = estimates
                updateTravelEstimates(estimates, for: maneuver)

                // Update upcoming maneuvers
                upcomingManeuvers = steps.map { _ in
                    let upcomingManeuver = CPManeuver()
                    // Configure maneuver based on step
                    return upcomingManeuver
                }
            }
            .store(in: &cancellables)
    }
}

// MARK: - CPNavigationSessionDelegate

extension CarPlayStateManager: CPNavigationSessionDelegate {
    func navigationSession(
        _: CPNavigationSession,
        didUpdateTravelEstimates _: CPTravelEstimates,
        for _: CPManeuver
    ) {
        // Handle any additional logic needed when estimates update
    }

    func navigationSession(
        _: CPNavigationSession,
        didPresent _: CPManeuver
    ) {
        // Handle when CarPlay presents a new maneuver
    }
}

// MARK: - FerrostarCoreDelegate

extension CarPlayStateManager: FerrostarCoreDelegate {
    func core(
        _: FerrostarCore,
        correctiveActionForDeviation _: Double,
        remainingWaypoints waypoints: [Waypoint]
    ) -> CorrectiveAction {
        .getNewRoutes(waypoints: waypoints)
    }

    func core(_: FerrostarCore, loadedAlternateRoutes routes: [Route]) {
        if let firstRoute = routes.first {
            try? ferrostarCore.startNavigation(route: firstRoute)
        }
    }
}
