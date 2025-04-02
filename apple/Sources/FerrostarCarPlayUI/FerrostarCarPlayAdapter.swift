import CarPlay
import Combine
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI

@MainActor
class FerrostarCarPlayAdapter: NSObject {
    // TODO: This should be customizable. For now we're just ignore it.
    @Published var uiState: CarPlayUIState = .idle(nil)

    private let ferrostarCore: FerrostarCore
    private let formatterCollection: FormatterCollection
    private let distanceUnits: MKDistanceFormatter.Units

    /// The MapTemplate hosts both the idle and navigating templates.
    private var mapTemplate: CPMapTemplate?
    private var idleTemplate: IdleMapTemplate?
    private var navigatingTemplate: NavigatingTemplateHost?

    var currentSession: CPNavigationSession?
    var currentTrip: CPTrip?

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
        onCenter: @escaping () -> Void
    ) {
        navigatingTemplate = NavigatingTemplateHost(
            mapTemplate: mapTemplate,
            formatters: formatterCollection,
            units: distanceUnits,
            showCentering: showCentering, // TODO: Make this dynamic based on the camera state
            onCenter: onCenter
        )

        setupObservers()
    }

    // Add this function to initialize the idle template
    func setupIdleTemplate() {
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

    private func setupObservers() {
        ferrostarCore.$route
            .receive(on: DispatchQueue.main)
            .sink { [weak self] route in
                guard let self, let route else { return }

                do {
                    uiState = .navigating
                    try navigatingTemplate?.start(routes: [route], waypoints: route.waypoints)
                    print("CarPlay - started")
                } catch {
                    print("CarPlay - startup error: \(error)")
                }
            }
            .store(in: &cancellables)

        ferrostarCore.$state
            .receive(on: DispatchQueue.main)
            .sink { [weak self] navState in
                guard let self else { return }
                
                if let navState {
                    navigatingTemplate?.update(navigationState: navState)
                } else {
                    
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
