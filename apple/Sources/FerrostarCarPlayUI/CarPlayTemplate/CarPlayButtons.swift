import CarPlay
import FerrostarSwiftUI

/// A collection of buttons that can be used in  a Ferrostar CarPlay implementation.
public enum CarPlayMapButtons {
    public static func pan(
        action: @escaping () -> Void
    ) -> CPMapButton {
        let button = CPMapButton { _ in
            action()
        }
        button.image = UIImage(systemName: "arrow.up.and.down.and.arrow.left.and.right")
        return button
    }

    public static func zoomIn(
        action: @escaping () -> Void
    ) -> CPMapButton {
        let button = CPMapButton { _ in
            action()
        }
        button.image = UIImage(systemName: "plus.circle.fill")
        return button
    }

    public static func zoomOut(
        action: @escaping () -> Void
    ) -> CPMapButton {
        let button = CPMapButton { _ in
            action()
        }
        button.image = UIImage(systemName: "minus.circle.fill")
        return button
    }

    public static func camera(
        _ state: CameraControlState
    ) -> CPMapButton? {
        switch state {
        case .hidden:
            return nil
        case let .showRecenter(action),
             let .showRouteOverview(action):
            let button = CPMapButton { _ in
                action()
            }
            button.image = UIImage(systemName: state.systemImageName)
            return button
        }
    }

    public static func toggleMute(
        _ isMuted: Bool,
        action: @escaping () -> Void
    ) -> CPMapButton {
        let button = CPMapButton { _ in
            action()
        }

        let iconName = isMuted ? "speaker.fill" : "speaker.3.fill"
        button.image = UIImage(systemName: iconName)
        return button
    }
}
