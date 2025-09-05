import CarPlay
import FerrostarCarPlayUI
import FerrostarCore
import MapLibreSwiftUI
import os
import SwiftUI
import UIKit

private let logger = Logger(subsystem: "DemoApp", category: "CarPlaySceneDelegate")

class CarPlaySceneDelegate: NSObject, CPTemplateApplicationSceneDelegate {
    func templateApplicationScene(
        _: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController,
        to window: CPWindow
    ) {
        logger.debug("\(#function)")
        CarPlaySession.shared.attach(interfaceController, to: window)
    }

    func templateApplicationScene(
        _: CPTemplateApplicationScene,
        didDisconnect _: CPInterfaceController,
        from window: CPWindow
    ) {
        logger.debug("\(#function)")
        // A device handoff could occur here if needed. In the
        // case of this demo app, the navigation session is mirrored on the device screen,
        // so no action is needed.
        window.isHidden = true
    }
}
