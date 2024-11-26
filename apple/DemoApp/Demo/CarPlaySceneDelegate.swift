import CarPlay
import FerrostarCarPlayUI
import FerrostarCore
import SwiftUI
import UIKit

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
    func templateApplicationDashboardScene(_: CPTemplateApplicationDashboardScene,
                                           didConnect _: CPDashboardController,
                                           to _: UIWindow) {}

    func templateApplicationDashboardScene(_: CPTemplateApplicationDashboardScene,
                                           didDisconnect _: CPDashboardController,
                                           from _: UIWindow) {}
}

extension CarPlaySceneDelegate: CPTemplateApplicationInstrumentClusterSceneDelegate {
    // swiftlint:disable identifier_name vertical_parameter_alignment
    func templateApplicationInstrumentClusterScene(
        _: CPTemplateApplicationInstrumentClusterScene,
        didConnect _: CPInstrumentClusterController
    ) {}

    func templateApplicationInstrumentClusterScene(
        _: CPTemplateApplicationInstrumentClusterScene,
        didDisconnectInstrumentClusterController _: CPInstrumentClusterController
    ) {}
    // swiftlint:enable identifier_name vertical_parameter_alignment
}
