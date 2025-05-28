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

class CarPlaySceneDelegate: NSObject, CPTemplateApplicationSceneDelegate {
    private var carPlayManager: FerrostarCarPlayManager?

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to window: CPWindow
    ) {
        Logger.carPlay.info("\(#function)")
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
        Logger.carPlay.info("\(#function)")
        carPlayManager?.templateApplicationScene(
            templateApplicationScene, didDisconnect: interfaceController, from: window
        )
    }

    func setupCarPlay(on window: UIWindow) {
        guard carPlayManager == nil else { return }

        let view = DemoCarPlayNavigationView(
            ferrostarCore: appEnvironment.ferrostarCore,
            styleURL: AppDefaults.mapStyleURL,
            camera: Binding(
                get: { appEnvironment.camera.camera },
                set: { appEnvironment.camera.camera = $0 }
            )
        )

        let carPlayViewController = UIHostingController(rootView: view)

        carPlayManager = FerrostarCarPlayManager(
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
