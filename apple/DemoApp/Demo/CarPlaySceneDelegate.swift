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

        guard let model = demoModel else {
            Logger.carPlay.error("No shared DemoModel")
            return
        }

        let manager = setupCarPlay(on: window, model: model)
        let mapTemplate = manager.mapTemplate

        // Set the root template
        interfaceController.setRootTemplate(mapTemplate, animated: true) { success, error in
            if let error {
                Logger.carPlay.error("Failed setRootTemplet: \(error)")
            } else {
                Logger.carPlay.debug("Template presented: \(success)")
            }
        }

        templateApplicationScene.session.carPlayManager = manager
    }

    public func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didDisconnect _: CPInterfaceController,
        from _: CPWindow
    ) {
        Logger.carPlay.debug("\(#function)")

        guard let manager = templateApplicationScene.session.carPlayManager else {
            Logger.carPlay.error("CarPlay not connected?")
            return
        }

        manager.disconnect()

        templateApplicationScene.session.carPlayManager = nil
    }

    private func setupCarPlay(on window: UIWindow, model: DemoModel) -> FerrostarCarPlayManager {
        let view = DemoCarPlayNavigationView(model: model, styleURL: AppDefaults.mapStyleURL)

        let carPlayViewController = UIHostingController(rootView: view)

        let carPlayManager = FerrostarCarPlayManager(
            model.core,
            distanceUnits: .default,
            // TODO: We may need to hold the view or viewController here, but it seems
            //       to work for now.
            showCentering: !model.camera.isTrackingUserLocationWithCourse,
            onCenter: { model.camera = .automotiveNavigation(pitch: 25)
            },
            onStartTrip: {
                // TODO: This will require some work on the FerrostarCore side - to accept a route before starting.
            },
            onCancelTrip: {
                model.core.stopNavigation()
            }
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
