import Foundation
import UIKit
import SwiftUI
import CarPlay
import FerrostarCore
import FerrostarMapLibreUI

@MainActor
public class FerrostarCarPlayManager: NSObject, CPInterfaceControllerDelegate, CPSessionConfigurationDelegate {

    public static let shared = FerrostarCarPlayManager()

    private var interfaceController: CPInterfaceController?
    private var carWindow: CPWindow?
    private var mapTemplate: CPMapTemplate?
    
    let ferrostarCore: FerrostarCore
    
    // MARK: CarPlay Controller & Windows

    private var carplayInterfaceController: CPInterfaceController?
    private var carWindow: UIWindow?
    private var instrumentClusterWindow: UIWindow?

//    private(set) var mapTemplate: MapTemplate!

    var currentTravelEstimates: CPTravelEstimates?
    var navigationSession: CPNavigationSession?
    var displayLink: CADisplayLink?
    var activeManeuver: CPManeuver?
    var activeEstimates: CPTravelEstimates?
    var lastCompletedManeuverFrame: CGRect?
    var sessionConfiguration: CPSessionConfiguration!

    // MARK: ViewControllers
    private var viewController: UIHostingController<CarPlayNavigationView>!

    init(ferrostarCore: FerrostarCore) {
        
    }
    
    func configureMainWindow(cpWindow: CPWindow) {
        
    }
    
    func configureInstrumenCluster() {
        
    }
    
    
    
    public override init() {
        super.init()

        viewController = UIHostingController(rootView: CarPlayNavigationView())
        sessionConfiguration = CPSessionConfiguration(delegate: self)

//        mainMapViewController.mapViewActionProvider = self
    }

    // MARK: CPTemplateApplicationSceneDelegate

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
        window.rootViewController = viewController
        
        // Create a map template and set it as the root.
        let mapTemplate = self.makeMapTemplate()
        interfaceController.setRootTemplate(mapTemplate, animated: true,
            completion: nil)
    }
    
    public func interfaceController(_ interfaceController: CPInterfaceController, didDisconnectWith window: CPWindow) {
        carplayInterfaceController = nil
        carWindow?.isHidden = true
    }

    // MARK: InterfaceControllerDelegate

    public func instrumentClusterControllerDidConnect(_ instrumentClusterWindow: UIWindow) {

    }

    public func instrumentClusterControllerDidDisconnectWindow(_ instrumentClusterWindow: UIWindow) {

    }

}
