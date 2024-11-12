import CarPlay
import UIKit
import SwiftUI
import FerrostarCarPlayUI

class CarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate {
    private var interfaceController: CPInterfaceController?
    private var carWindow: CPWindow?
    private var mapTemplate: CPMapTemplate?
    
    init(ferrostarCore: FerrostarCore) {
        
    }
    
    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to window: CPWindow
    ) {
        
        // Retain references to the interface controller and window for
        // the entire duration of the CarPlay session.
        self.interfaceController = interfaceController
        self.carWindow = window
        
        // Assign the window's root view controller to the view controller
        // that draws your map content.
        window.rootViewController = UIHostingController(rootView: CarPlayNavigationView())
        
        // Create a map template and set it as the root.
        let mapTemplate = self.makeMapTemplate()
        interfaceController.setRootTemplate(mapTemplate, animated: true,
            completion: nil)
    }
    
    func makeMapTemplate() -> CPMapTemplate {
        let mapTemplate = CPMapTemplate()
        
        
        return mapTemplate
    }
}

extension CarPlaySceneDelegate: CPTemplateApplicationDashboardSceneDelegate {

    func templateApplicationDashboardScene(_ templateApplicationDashboardScene: CPTemplateApplicationDashboardScene,
                                           didConnect dashboardController: CPDashboardController,
                                           to window: UIWindow) {

    }

    func templateApplicationDashboardScene(_ templateApplicationDashboardScene: CPTemplateApplicationDashboardScene,
                                           didDisconnect dashboardController: CPDashboardController,
                                           from window: UIWindow) {

    }

}

extension CarPlaySceneDelegate: CPTemplateApplicationInstrumentClusterSceneDelegate {

    // swiftlint:disable identifier_name vertical_parameter_alignment
    func templateApplicationInstrumentClusterScene(
        _ templateApplicationInstrumentClusterScene: CPTemplateApplicationInstrumentClusterScene,
        didConnect instrumentClusterController: CPInstrumentClusterController
    ) {

    }

    func templateApplicationInstrumentClusterScene(
        _ templateApplicationInstrumentClusterScene: CPTemplateApplicationInstrumentClusterScene,
       didDisconnectInstrumentClusterController instrumentClusterController: CPInstrumentClusterController
    ) {

    }
    // swiftlint:enable identifier_name vertical_parameter_alignment
}
