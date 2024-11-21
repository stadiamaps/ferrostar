import Foundation
import UIKit
import SwiftUI
import CarPlay
import MapLibreSwiftUI
import FerrostarCore
import FerrostarMapLibreUI

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
//    var activeManeuver: CPManeuver?
//    var activeEstimates: CPTravelEstimates?
//    var lastCompletedManeuverFrame: CGRect?
    
    private let ferrostarCore: FerrostarCore
    private let styleURL: URL
    
    private var viewController: UIHostingController<AnyView>!

    public init(
        ferrostarCore: FerrostarCore,
        styleURL: URL
    ) {
        self.ferrostarCore = ferrostarCore
        self.styleURL = styleURL
        
        super.init()
        
//        sessionConfiguration = CPSessionConfiguration(delegate: self)
    }
    
    public func templateApplicationScene(
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
        window.rootViewController = UIHostingController {
            CarPlayNavigationView(styleURL: styleURL)
                .environmentObject(ferrostarCore)
        }
        
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

extension FerrostarCarPlayManager: CPTemplateApplicationDashboardSceneDelegate {

    public func templateApplicationDashboardScene(
        _ templateApplicationDashboardScene: CPTemplateApplicationDashboardScene,
        didConnect dashboardController: CPDashboardController,
        to window: UIWindow
    ) {

    }

    public func templateApplicationDashboardScene(
        _ templateApplicationDashboardScene: CPTemplateApplicationDashboardScene,
        didDisconnect dashboardController: CPDashboardController,
        from window: UIWindow
    ) {

    }

}

extension FerrostarCarPlayManager: CPTemplateApplicationInstrumentClusterSceneDelegate {
    public func templateApplicationInstrumentClusterScene(
        _ templateApplicationInstrumentClusterScene: CPTemplateApplicationInstrumentClusterScene,
        didConnect instrumentClusterController: CPInstrumentClusterController
    ) {

    }

    public func templateApplicationInstrumentClusterScene(
        _ templateApplicationInstrumentClusterScene: CPTemplateApplicationInstrumentClusterScene,
       didDisconnectInstrumentClusterController instrumentClusterController: CPInstrumentClusterController
    ) {

    }
}

