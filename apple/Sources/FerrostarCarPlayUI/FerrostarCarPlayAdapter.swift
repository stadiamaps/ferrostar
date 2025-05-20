import CarPlay
import Combine
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI

class FerrostarCarPlayAdapter: NSObject {
    // TODO: This should be customizable. For now we're just ignore it.
    private var uiState: CarPlayUIState = .idle(nil)

    private let ferrostarCore: FerrostarCore
    private let formatterCollection: FormatterCollection
    private let distanceUnits: MKDistanceFormatter.Units

    /// The MapTemplate hosts both the idle and navigating templates.
    private var idleTemplate: IdleMapTemplate?
    private var navigatingTemplate: NavigatingTemplateHost?

    private var cancellables = Set<AnyCancellable>()

    init(
        ferrostarCore: FerrostarCore,
        formatterCollection: FormatterCollection = FoundationFormatterCollection(),
        distanceUnits: MKDistanceFormatter.Units = .default
    ) {
        self.ferrostarCore = ferrostarCore
        self.formatterCollection = formatterCollection
        self.distanceUnits = distanceUnits

        super.init()
        setupIdleTemplate()
    }

    func setup(
        on mapTemplate: CPMapTemplate,
        showCentering: Bool,
        onCenter: @escaping () -> Void,
        onStartTrip: @escaping () -> Void,
        onCancelTrip: @escaping () -> Void
    ) {
        navigatingTemplate = NavigatingTemplateHost(
            mapTemplate: mapTemplate,
            formatters: formatterCollection,
            units: distanceUnits,
            showCentering: showCentering, // TODO: Make this dynamic based on the camera state
            onCenter: onCenter,
            onStartTrip: onStartTrip,
            onCancelTrip: onCancelTrip
        )

        setupObservers()
    }

    // Add this function to initialize the idle template
    func setupIdleTemplate() {
        // TODO: Review https://developer.apple.com/carplay/documentation/CarPlay-App-Programming-Guide.pdf
        //       Page 37 of 65 - we probably want the default idle template to be a trip preview w/ start nav.
        idleTemplate = IdleMapTemplate()

        idleTemplate?.onSearchButtonTapped = { [weak self] in
            // Handle search
        }

        idleTemplate?.onRecenterButtonTapped = { [weak self] in
            // Handle recenter
        }

        idleTemplate?.onStartNavigationButtonTapped = { [weak self] in
            // Handle start navigation
        }
    }

    private func terminateTrip(cancelled: Bool = false) {
        if let navigatingTemplate {
            if cancelled {
                navigatingTemplate.cancelTrip()
            } else {
                navigatingTemplate.completeTrip()
            }
        }
        uiState = .idle(nil)
    }

    private func setupObservers() {
        // Handle Navigation Start/Stop
        Publishers.CombineLatest(
            ferrostarCore.$route,
            ferrostarCore.$state
        )
        .receive(on: DispatchQueue.main)
        .sink { [weak self] route, navState in
            guard let self else { return }
            guard let navState else {
                if case let .navigating = self.uiState {
                    terminateTrip(cancelled: true)
                }
                return
            }

            switch navState.tripState {
            case .navigating:
                if let route, uiState != .navigating {
                    uiState = .navigating
                    do {
                        try navigatingTemplate?.start(routes: [route], waypoints: route.waypoints)
                        print("CarPlay - started")
                    } catch {
                        print("CarPlay - startup error: \(error)")
                    }
                }
                navigatingTemplate?.update(navigationState: navState)
            case .complete:
                terminateTrip()
            case .idle:
                break
            }
        }
        .store(in: &cancellables)

        ferrostarCore.$state
            .receive(on: DispatchQueue.main)
            .compactMap { navState -> (VisualInstruction, RouteStep)? in
                guard let instruction = navState?.currentVisualInstruction,
                      let step = navState?.currentStep
                else {
                    return nil
                }

                return (instruction, step)
            }
            .removeDuplicates(by: { $0.0 == $1.0 })
            .sink { [weak self] instruction, step in
                guard let self else { return }

                navigatingTemplate?.update(instruction, currentStep: step)
            }
            .store(in: &cancellables)
    }
}
