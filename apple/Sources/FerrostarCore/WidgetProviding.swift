import FerrostarCoreFFI

/// The widget provided is given to ``FerrostarCore/FerrostarCore`` and is called when the navigation state is updated
/// by a user
/// update or navitation is stopped.
public protocol WidgetProviding {
    /// Update _or create_ the widget with a new status
    ///
    /// - Parameters:
    ///   - visualInstruction: The latest visual instruction provided by the navigation state.
    ///   - tripProgress: The latest trip progress provided by the navigation state.
    func update(visualInstruction: VisualInstruction, tripProgress: TripProgress)

    /// Terminate the session.
    func terminate()
}
