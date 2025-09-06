import CarPlay
import Combine
import SwiftUI

@Observable
class DemoCarPlayNavController: NSObject {
    var path = NavigationPath()

    private(set) var currentScene: DemoCarPlayScene? = .search

    func navigate(to scene: DemoCarPlayScene) {
        path.append(scene)
        currentScene = scene
    }

    func navigateBack() {
        path.removeLast()
    }

    func navigateToRoot() {
        path.removeLast(path.count)
    }
}

extension DemoCarPlayNavController: CPInterfaceControllerDelegate {
    func templateWillDisappear(_ aTemplate: CPTemplate, animated _: Bool) {
        if aTemplate is CPSearchTemplate {
            navigateBack()
        }
    }
}

private struct CarPlayNavControllerKey: EnvironmentKey {
    nonisolated(unsafe) static let defaultValue = DemoCarPlayNavController()
}

extension EnvironmentValues {
    var carPlayNavController: DemoCarPlayNavController {
        get { self[CarPlayNavControllerKey.self] }
        set { self[CarPlayNavControllerKey.self] = newValue }
    }
}
