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

private let ModelKey = "com.stadiamaps.ferrostar.model"

private extension UISceneSession {
    var model: DemoCarPlayModel? {
        get {
            userInfo?[ModelKey] as? DemoCarPlayModel
        }
        set {
            var info = userInfo ?? [:]
            info[ModelKey] = newValue
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

        guard templateApplicationScene.session.model == nil else {
            Logger.carPlay.error("CarPlay already connected?")
            return
        }

        guard let model = demoModel else {
            Logger.carPlay.error("No shared DemoModel")
            return
        }

        let carPlayModel = DemoCarPlayModel(model: model, interfaceController: interfaceController)
        templateApplicationScene.session.model = carPlayModel

        let vc = CarPlayHostingController {
            DemoCarPlayNavigationView(model: carPlayModel)
        }
        window.rootViewController = vc
        window.makeKeyAndVisible()

        let mapTemplate = carPlayModel.createAndAttachTemplate()

        Task { @MainActor in
            do {
                _ = try await interfaceController.setRootTemplate(mapTemplate, animated: true)
            } catch {
                Logger.carPlay.error("Cannot setRootTemplate")
                carPlayModel.errorMessage = error.localizedDescription
            }
        }
    }

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didDisconnect _: CPInterfaceController,
        from window: CPWindow
    ) {
        Logger.carPlay.debug("\(#function)")

        guard let model = templateApplicationScene.session.model else {
            Logger.carPlay.error("CarPlay not connected?")
            return
        }

        model.stop(cancelTrip: true, mapTemplate: nil)
        window.isHidden = true

        templateApplicationScene.session.model = nil
    }
}
