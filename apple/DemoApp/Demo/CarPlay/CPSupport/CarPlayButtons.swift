import CarPlay

enum NavigationBarButtons {
    static func search(
        action: @escaping () -> Void
    ) -> CPBarButton {
        let button = CPBarButton(title: "Search") { _ in
            action()
        }
        return button
    }

    static func stop(
        action: @escaping () -> Void
    ) -> CPBarButton {
        let button = CPBarButton(title: "Stop") { _ in
            action()
        }
        return button
    }
}

enum MapButtons {
    static func zoomIn(
        action: @escaping () -> Void
    ) -> CPMapButton {
        let button = CPMapButton { _ in
            action()
        }
        button.image = UIImage(systemName: "plus.circle.fill")
        return button
    }

    static func zoomOut(
        action: @escaping () -> Void
    ) -> CPMapButton {
        let button = CPMapButton { _ in
            action()
        }
        button.image = UIImage(systemName: "minus.circle.fill")
        return button
    }

    static func centerOn(
        _ centerOnRoute: Bool,
        action: @escaping () -> Void
    ) -> CPMapButton {
        let button = CPMapButton { _ in
            action()
        }
        let iconName = centerOnRoute ? "point.bottomleft.forward.to.point.topright.scurvepath" : "location.north.line"
        button.image = UIImage(systemName: iconName)
        return button
    }

    static func toggleMute(
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
