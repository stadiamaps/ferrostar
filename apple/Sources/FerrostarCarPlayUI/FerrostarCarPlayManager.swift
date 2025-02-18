import CarPlay
import Combine
import FerrostarCore
import FerrostarMapLibreUI
import Foundation
import MapLibreSwiftUI
import SwiftUI
import UIKit

@MainActor
public class FerrostarCarPlayManager: NSObject, CPInterfaceControllerDelegate, CPSessionConfigurationDelegate {
    // MARK: CarPlay Controller & Windows

    private var sessionConfiguration: CPSessionConfiguration!

    private var interfaceController: CPInterfaceController?
    private var carWindow: CPWindow?

    private var mapTemplate: CPMapTemplate?

//    private var instrumentClusterWindow: UIWindow?
//    var currentTravelEstimates: CPTravelEstimates?
//    var navigationSession: CPNavigationSession?
//    var displayLink: CADisplayLink?
//    var activeEstimates: CPTravelEstimates?
//    var lastCompletedManeuverFrame: CGRect?

    private let ferrostarCore: FerrostarCore
    private let stateManager: CarPlayStateManager
    private var cancellables = Set<AnyCancellable>()

    private let styleURL: URL

    private var viewController: UIHostingController<AnyView>!

    public init(
        ferrostarCore: FerrostarCore,
        styleURL: URL
    ) {
        self.ferrostarCore = ferrostarCore
        stateManager = CarPlayStateManager(ferrostarCore: ferrostarCore)
        self.styleURL = styleURL

        super.init()

        stateManager.$currentManeuver
            .sink { [weak self] maneuver in
                guard let self, let mapTemplate else { return }

                if let maneuver {
                    // If there's no navigation session, create one
                    if mapTemplate.navigationSession == nil {
                        mapTemplate.startNavigationSession()
                    }

                    // Update the current maneuver
                    mapTemplate.navigationSession?.upcomingManeuvers = [maneuver]
                } else {
                    // If there's no maneuver, end the navigation session
                    mapTemplate.navigationSession?.finish()
                }
            }
            .store(in: &cancellables)

//        sessionConfiguration = CPSessionConfiguration(delegate: self)
    }

    public func templateApplicationScene(
        _: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to window: CPWindow
    ) {
        // Retain references to the interface controller and window for
        // the entire duration of the CarPlay session.
        self.interfaceController = interfaceController
        carWindow = window

        // Assign the window's root view controller to the view controller
        // that draws your map content.
        window.rootViewController = UIHostingController {
            CarPlayNavigationView(styleURL: styleURL)
                .environmentObject(ferrostarCore)
        }

        // Create a map template and set it as the root.
        let mapTemplate = makeMapTemplate()
        interfaceController.setRootTemplate(mapTemplate, animated: true,
                                            completion: nil)
    }

    func makeMapTemplate() -> CPMapTemplate {
        let mapTemplate = CPMapTemplate()
        return mapTemplate
    }
}

@MainActor
extension FerrostarCarPlayManager: CPTemplateApplicationDashboardSceneDelegate {
    public func templateApplicationDashboardScene(
        _: CPTemplateApplicationDashboardScene,
        didConnect _: CPDashboardController,
        to _: UIWindow
    ) {}

    public func templateApplicationDashboardScene(
        _: CPTemplateApplicationDashboardScene,
        didDisconnect _: CPDashboardController,
        from _: UIWindow
    ) {}
}

@MainActor
extension FerrostarCarPlayManager: CPTemplateApplicationInstrumentClusterSceneDelegate {
    public func templateApplicationInstrumentClusterScene(
        _: CPTemplateApplicationInstrumentClusterScene,
        didConnect _: CPInstrumentClusterController
    ) {}

    public func templateApplicationInstrumentClusterScene(
        _: CPTemplateApplicationInstrumentClusterScene,
        didDisconnectInstrumentClusterController _: CPInstrumentClusterController
    ) {}
}
