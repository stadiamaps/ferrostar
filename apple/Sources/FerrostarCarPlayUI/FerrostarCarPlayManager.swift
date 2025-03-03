import CarPlay
import FerrostarCore
import Foundation

public class FerrostarCarPlayManager: NSObject, CPTemplateApplicationSceneDelegate {
    private let ferrostarCore: FerrostarCore
    private let distanceUnits: MKDistanceFormatter.Units

    private var ferrostarAdapter: FerrostarCarPlayAdapter?
    private var interfaceController: CPInterfaceController?

    public init(
        _ ferrostartCore: FerrostarCore,
        distanceUnits: MKDistanceFormatter.Units
    ) {
        ferrostarCore = ferrostartCore
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
        ferrostarAdapter?.setup(on: mapTemplate)

        // Set the root template
        interfaceController.setRootTemplate(mapTemplate, animated: true)
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
