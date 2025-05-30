import CarPlay
import FerrostarCore
import Foundation
import MapLibreSwiftUI
import os
import OSLog
import SwiftUI

private extension Logger {
    static let cpMapTemplateDelegate = Logger(category: "CPMapTemplateDelegate")
}

public class FerrostarCarPlayManager: NSObject, CPTemplateApplicationSceneDelegate {
    private let logger: Logger

    private var ferrostarAdapter: FerrostarCarPlayAdapter
    private var interfaceController: CPInterfaceController?

    private var mapTemplate: CPMapTemplate = .init()

    public init(
        _ ferrostarCore: FerrostarCore,
        logger: Logger = Logger(
            subsystem: Bundle.main.bundleIdentifier ?? "FerrostarCarPlayUI",
            category: "FerrostarCarPlayManager"
        ),
        distanceUnits: MKDistanceFormatter.Units,
        showCentering: Bool,
        onCenter: @escaping () -> Void,
        onStartTrip: @escaping () -> Void,
        onCancelTrip: @escaping () -> Void
    ) {
        self.logger = logger

        // Create the navigation adapter
        ferrostarAdapter = FerrostarCarPlayAdapter(ferrostarCore: ferrostarCore,
                                                   distanceUnits: distanceUnits,
                                                   mapTemplate: mapTemplate,
                                                   showCentering: showCentering,
                                                   onCenter: onCenter,
                                                   onStartTrip: onStartTrip,
                                                   onCancelTrip: onCancelTrip)

        super.init()
    }

    // MARK: CPApplicationDelegate

    public func templateApplicationScene(
        _: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to _: CPWindow
    ) {
        logger.debug("\(#function)")
        self.interfaceController = interfaceController

        // Set the root template
        interfaceController.setRootTemplate(mapTemplate, animated: true) { [weak self] success, error in
            if let error {
                self?.logger.error("Failed didConnect to CPWindow with error: \(error)")
            } else {
                self?.logger.debug("Connected to CPWindow - template presented: \(success)")
            }
        }
    }

    public func templateApplicationScene(
        _: CPTemplateApplicationScene,
        didDisconnect _: CPInterfaceController,
        from _: CPWindow
    ) {
        logger.debug("\(#function)")
        interfaceController = nil
    }
}

extension FerrostarCarPlayManager: CPMapTemplateDelegate {
    public func mapTemplate(_: CPMapTemplate, selectedPreviewFor _: CPTrip, using _: CPRouteChoice) {
        Logger.cpMapTemplateDelegate.debug("\(#function)")
        // TODO: What is this for?
    }

    public func mapTemplate(_: CPMapTemplate, startedTrip _: CPTrip, using _: CPRouteChoice) {
        Logger.cpMapTemplateDelegate.debug("\(#function)")
        // TODO: What is this for?
    }
}
