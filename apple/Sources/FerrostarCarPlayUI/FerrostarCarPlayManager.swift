import CarPlay
import Combine
import FerrostarCore
import FerrostarCoreFFI
import FerrostarSwiftUI
import Foundation
import MapLibreSwiftUI
import os
import OSLog
import SwiftUI

public class FerrostarCarPlayManager: NSObject {
    private let logger: Logger

    // TODO: This should be customizable. For now we're just ignore it.
    private var uiState: CarPlayUIState = .idle(nil)
    private let ferrostarCore: FerrostarCore
    private var navigatingTemplate: NavigatingTemplateHost
    private var cancellables = Set<AnyCancellable>()

    public var mapTemplate: CPMapTemplate = .init()

    public init(
        _ ferrostarCore: FerrostarCore,
        logger: Logger = Logger(
            subsystem: Bundle.main.bundleIdentifier ?? "FerrostarCarPlayUI",
            category: "FerrostarCarPlayManager"
        ),
        formatterCollection: FormatterCollection = FoundationFormatterCollection(),
        distanceUnits: MKDistanceFormatter.Units,
        showCentering: Bool,
        onCenter: @escaping () -> Void,
        onStartTrip: @escaping () -> Void,
        onCancelTrip: @escaping () -> Void
    ) {
        self.ferrostarCore = ferrostarCore
        self.logger = logger

        navigatingTemplate = NavigatingTemplateHost(
            mapTemplate: mapTemplate,
            formatters: formatterCollection,
            units: distanceUnits,
            showCentering: showCentering, // TODO: Make this dynamic based on the camera state
            onCenter: onCenter,
            onStartTrip: onStartTrip,
            onCancelTrip: onCancelTrip
        )

        super.init()
        setupObservers()
    }

    public func disconnect() {
        logger.debug("\(#function)")
    }

    private func terminateTrip(cancelled: Bool = false) {
        if cancelled {
            navigatingTemplate.cancelTrip()
        } else {
            navigatingTemplate.completeTrip()
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
                        try navigatingTemplate.start(routes: [route], waypoints: route.waypoints)
                        logger.debug("CarPlay - started")
                    } catch {
                        logger.debug("CarPlay - startup error: \(error, privacy: .public)")
                    }
                }
                navigatingTemplate.update(navigationState: navState)
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

                navigatingTemplate.update(instruction, currentStep: step)
            }
            .store(in: &cancellables)
    }
}
