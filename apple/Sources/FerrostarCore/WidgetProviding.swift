import FerrostarCoreFFI

/// The widget provided is given to ``FerrostarCore/FerrostarCore`` and is called when the navigation state is updated
/// by a user update or navigation is stopped.
public protocol WidgetProviding {
    /// Update _or create_ the widget with a new status
    ///
    /// - Parameters:
    ///   - visualInstruction: The latest visual instruction provided by the navigation state.
    ///   - spokenInstruction: An optional spoken instruction if there is one. This will trigger an alert to wake the
    /// screen on the update.
    ///   - tripProgress: The latest trip progress provided by the navigation state.
    func update(visualInstruction: VisualInstruction, spokenInstruction: SpokenInstruction?, tripProgress: TripProgress)

    /// Terminate the session.
    func terminate()
}
