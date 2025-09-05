import CarPlay

extension CPNavigationAlert {
    static func refresh(
        _ action: @escaping () -> Void
    ) -> CPNavigationAlert {
        CPNavigationAlert(
            titleVariants: ["Refresh with Mobile Screen"],
            subtitleVariants: [
                "If you use the mobile devie to load a route, you can use this to manually refresh.",
            ],
            image: nil,
            primaryAction: .init(title: "Refresh", color: .orange, handler: { _ in
                action()
            }),
            secondaryAction: nil,
            duration: .init()
        )
    }
}
