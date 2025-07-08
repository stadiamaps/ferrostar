import CarPlay

enum CarPlayMapButtons {
    static func recenterButton(
        isHidden: Bool = false,
        isEnabled: Bool = true,
        _ action: @escaping () -> Void
    ) -> CPMapButton {
        let button = CPMapButton { _ in
            action()
        }

        button.image = UIImage(systemName: "location.north.fill")
        button.isHidden = isHidden
        button.isEnabled = isEnabled
        return button
    }
}

enum CarPlayBarButtons {
    static func startNavigationButton(_ action: @escaping () -> Void) -> CPBarButton {
        let button = CPBarButton(title: String(localized: "start_nav")) { _ in
            action()
        }
        return button
    }

    static func cancelNavigationButton(_ action: @escaping () -> Void) -> CPBarButton {
        let button = CPBarButton(title: String(localized: "cancel")) { _ in
            action()
        }
        return button
    }
}
