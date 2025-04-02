import CarPlay
import FerrostarCore
import Foundation
import MapLibreSwiftUI
import OSLog
import SwiftUI

public class FerrostarCarPlayManager: NSObject, CPTemplateApplicationSceneDelegate {
    private let ferrostarCore: FerrostarCore
    @Binding var camera: MapViewCamera

    private let logger: Logger
    private let distanceUnits: MKDistanceFormatter.Units

    private var ferrostarAdapter: FerrostarCarPlayAdapter?
    private var interfaceController: CPInterfaceController?

    public init(
        _ ferrostarCore: FerrostarCore,
        camera: Binding<MapViewCamera>,
        logger: Logger = Logger(
            subsystem: Bundle.main.bundleIdentifier ?? "FerrostarCarPlayUI",
            category: "FerrostarCarPlayManager"
        ),
        distanceUnits: MKDistanceFormatter.Units
    ) {
        self.ferrostarCore = ferrostarCore
        _camera = camera
        self.logger = logger
        self.distanceUnits = distanceUnits

        super.init()
    }

    // MARK: CPApplicationDelegate

    public func templateApplicationScene(
        _: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to _: CPWindow
    ) {
        self.interfaceController = interfaceController

        // Create the map template
        let mapTemplate = CPMapTemplate()

        // Create the navigation adapter
        ferrostarAdapter = FerrostarCarPlayAdapter(ferrostarCore: ferrostarCore,
                                                   distanceUnits: distanceUnits)

        ferrostarAdapter?.setup(
            on: mapTemplate,
            showCentering: !camera.isTrackingUserLocationWithCourse,
            onCenter: { [weak self] in
                self?.camera = .automotiveNavigation()
            }
        )

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
        interfaceController = nil
        ferrostarAdapter = nil
    }
}

extension FerrostarCarPlayManager: CPMapTemplateDelegate {
    public func mapTemplate(_: CPMapTemplate, selectedPreviewFor _: CPTrip, using _: CPRouteChoice) {
        // TODO: What is this for?
    }

    public func mapTemplate(_: CPMapTemplate, startedTrip _: CPTrip, using _: CPRouteChoice) {
        // TODO: What is this for?
    }
}
