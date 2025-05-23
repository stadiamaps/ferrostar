import CarPlay
import FerrostarCarPlayUI
import FerrostarCore
import MapLibreSwiftUI
import os
import SwiftUI
import UIKit

private extension Logger {
    static let carPlay = Logger(subsystem: "ferrostar", category: "carplaydelegate")
}

private let CarPlaySceneDelegateKey = "ferrostar"

private extension UISceneSession {
    var carPlayManager: FerrostarCarPlayManager? {
        get {
            userInfo?[CarPlaySceneDelegateKey] as? FerrostarCarPlayManager
        }
        set {
            var info = userInfo ?? [:]
            info[CarPlaySceneDelegateKey] = newValue
            userInfo = info
        }
    }
}

class CarPlaySceneDelegate: NSObject, CPTemplateApplicationSceneDelegate {
    private var carPlayViewController: UIViewController?

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to window: CPWindow
    ) {
        Logger.carPlay.debug("\(#function)")

        guard templateApplicationScene.session.carPlayManager == nil else {
            Logger.carPlay.error("CarPlay already connected?")
            return
        }

        let manager = setupCarPlay(on: window)
        manager.templateApplicationScene(
            templateApplicationScene, didConnect: interfaceController, to: window
        )

        templateApplicationScene.session.carPlayManager = manager
    }

    public func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didDisconnect interfaceController: CPInterfaceController,
        from window: CPWindow
    ) {
        Logger.carPlay.debug("\(#function)")

        guard let manager = templateApplicationScene.session.carPlayManager else {
            Logger.carPlay.error("CarPlay not connected?")
            return
        }

        manager.templateApplicationScene(
            templateApplicationScene, didDisconnect: interfaceController, from: window
        )

        templateApplicationScene.session.carPlayManager = nil
    }

    private func setupCarPlay(on window: UIWindow) -> FerrostarCarPlayManager {
        let view = DemoCarPlayNavigationView(
            ferrostarCore: appEnvironment.ferrostarCore,
            styleURL: AppDefaults.mapStyleURL,
            camera: Binding(
                get: { appEnvironment.camera.camera },
                set: { appEnvironment.camera.camera = $0 }
            )
        )

        carPlayViewController = UIHostingController(rootView: view)

        let carPlayManager = FerrostarCarPlayManager(
            appEnvironment.ferrostarCore,
            camera: Binding(
                get: { appEnvironment.camera.camera },
                set: { appEnvironment.camera.camera = $0 }
            ),
            distanceUnits: .default
            // TODO: We may need to hold the view or viewController here, but it seems
            //       to work for now.
        )

        window.rootViewController = carPlayViewController
        window.makeKeyAndVisible()

        return carPlayManager
    }
}

extension CarPlaySceneDelegate: CPTemplateApplicationDashboardSceneDelegate {
    func templateApplicationDashboardScene(
        _: CPTemplateApplicationDashboardScene,
        didConnect _: CPDashboardController,
        to _: UIWindow
    ) {
        Logger.carPlay.info("\(#function)")
    }

    func templateApplicationDashboardScene(
        _: CPTemplateApplicationDashboardScene,
        didDisconnect _: CPDashboardController,
        from _: UIWindow
    ) {
        Logger.carPlay.info("\(#function)")
    }
}

extension CarPlaySceneDelegate: CPTemplateApplicationInstrumentClusterSceneDelegate {
    // swiftlint:disable identifier_name vertical_parameter_alignment
    func templateApplicationInstrumentClusterScene(
        _: CPTemplateApplicationInstrumentClusterScene,
        didConnect _: CPInstrumentClusterController
    ) {
        Logger.carPlay.info("\(#function)")
    }

    func templateApplicationInstrumentClusterScene(
        _: CPTemplateApplicationInstrumentClusterScene,
        didDisconnectInstrumentClusterController _: CPInstrumentClusterController
    ) {
        Logger.carPlay.info("\(#function)")
    }
    // swiftlint:enable identifier_name vertical_parameter_alignment
}
