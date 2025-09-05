import CarPlay
import OSLog
import SwiftUI
import UIKit

private let logger = Logger(subsystem: "DemoApp", category: "CarPlaySession")

@MainActor
class CarPlaySession {
    static let shared = CarPlaySession()

    private(set) weak var interfaceController: CPInterfaceController?
    private var window: CPWindow?
    // TODO: Instrument Cluster Window

    private(set) var session: CPNavigationSession?

    func attach(_ interfaceController: CPInterfaceController, to window: CPWindow) {
        logger.debug("Attaching to interface and window")

        // Attach the RootView to the Window.
        let view = DemoCarPlayAppNavigation()
        let vc = UIHostingController(rootView: view)
        window.rootViewController = vc
        window.makeKeyAndVisible()

        self.interfaceController = interfaceController
        self.window = window
    }

    func registerDelegate(_ delegate: CPInterfaceControllerDelegate) {
        interfaceController!.delegate = delegate
    }

    func setRootTemplate(_ template: sending CPTemplate) async throws {
        try await interfaceController?.setRootTemplate(template, animated: true)
    }

    func pushTemplate(_ template: sending CPTemplate) async throws {
        try await interfaceController?.pushTemplate(template, animated: true)
    }

    func presentTemplate(_ template: sending CPTemplate) async throws {
        try await interfaceController?.presentTemplate(template, animated: true)
    }

    func dismissTemplate() async throws {
        try await interfaceController?.dismissTemplate(animated: true)
    }

    // MARK: Navigation Session

    func startNavigationSession(on mapTemplate: CPMapTemplate, trip: CPTrip) throws {
        guard session == nil else {
            throw DemoError.sessionNotInProgress
        }

        session = mapTemplate.startNavigationSession(for: trip)
    }

    func cancelNavigationSession() {
        session?.cancelTrip()
        session = nil
    }

    func finishNavigationSession() {
        session?.finishTrip()
        session = nil
    }
}
