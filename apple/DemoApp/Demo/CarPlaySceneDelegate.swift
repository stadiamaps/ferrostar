import CarPlay
import FerrostarCarPlayUI
import FerrostarCore
import SwiftUI
import UIKit

class CarPlaySceneDelegate: UIResponder, UIWindowSceneDelegate, CPTemplateApplicationSceneDelegate {
    // Get the AppDelegate associated with the SwiftUI App/@main as the type you defined it as.
    @UIApplicationDelegateAdaptor(DemoAppDelegate.self) var appDelegate

    private weak var ferrostarCore: FerrostarCore?
    private var carPlayViewController: UIViewController?

    private var carPlayManager: FerrostarCarPlayManager?

    func scene(
        _: UIScene, willConnectTo _: UISceneSession,
        options _: UIScene.ConnectionOptions
    ) {
        // NOTE: This can also be used to set up your App's window & CarPlay scene.
        //       This example just uses the car play specific templateApplicationScene(_:didConnect:to)
    }

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to window: CPWindow
    ) {
        setupCarPlay(on: window)
        carPlayManager?.templateApplicationScene(
            templateApplicationScene, didConnect: interfaceController, to: window
        )
    }

    public func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didDisconnect interfaceController: CPInterfaceController,
        from window: CPWindow
    ) {
        carPlayManager?.templateApplicationScene(
            templateApplicationScene, didDisconnect: interfaceController, from: window
        )
    }

    func setupCarPlay(on window: UIWindow) {
        guard carPlayManager == nil else { return }

        // IMPORTANT: This is your app's shared FerrostarCore
        ferrostarCore = appDelegate.appEnvironment.ferrostarCore

        let view = CarPlayNavigationView(
            ferrostarCore: ferrostarCore!,
            styleURL: AppDefaults.mapStyleURL
        )

        carPlayViewController = UIHostingController(rootView: view)

        carPlayManager = FerrostarCarPlayManager(
            ferrostarCore!,
            distanceUnits: .default
            // TODO: We may need to hold the view or viewController here, but it seems
            //       to work for now.
        )

        window.rootViewController = carPlayViewController
        window.makeKeyAndVisible()
    }
}

extension CarPlaySceneDelegate: CPTemplateApplicationDashboardSceneDelegate {
    func templateApplicationDashboardScene(
        _: CPTemplateApplicationDashboardScene,
        didConnect _: CPDashboardController,
        to _: UIWindow
    ) {}

    func templateApplicationDashboardScene(
        _: CPTemplateApplicationDashboardScene,
        didDisconnect _: CPDashboardController,
        from _: UIWindow
    ) {}
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
