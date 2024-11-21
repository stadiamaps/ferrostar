import CarPlay
import UIKit
import SwiftUI
import FerrostarCore
import FerrostarCarPlayUI

class CarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate {
    
    // Get the AppDelegate associated with the SwiftUI App/@main as the type you defined it as.
    @UIApplicationDelegateAdaptor(DemoAppDelegate.self) var appDelegate
    
    private var ferrostarManager: FerrostarCarPlayManager?
    
    func configure() {
        guard ferrostarManager == nil else { return }
        
        ferrostarManager = FerrostarCarPlayManager(
            ferrostarCore: appDelegate.appEnvironment.ferrostarCore,
            styleURL: AppDefaults.mapStyleURL
        )
    }
    
    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to window: CPWindow
    ) {
        configure()
        ferrostarManager!.templateApplicationScene(templateApplicationScene,
                                                   didConnect: interfaceController,
                                                   to: window)
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
