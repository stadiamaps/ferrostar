import CarPlay

enum CarPlayUIState {
    /// The idle map template should display the map before use.
    ///
    /// It accepts an optional template since it typically will require customization
    /// by the implementing app to do more than just show the map and start nav.
    case idle(CPTemplate?)

    /// The Ferrostar supplied navigation template.
    case navigating

    // TODO: What other cases should we offer for configuration?
}
