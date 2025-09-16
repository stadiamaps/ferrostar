/// Determines which camera control is shown in the navigation inner grid view.
public enum CameraControlState {
    /// Don't show any camera control.
    case hidden

    /// Shows the recenter button.
    ///
    /// The action is responsible for resetting the camera
    /// to a state that follows the user.
    case showRecenter(() -> Void)

    /// Shows the route overview button.
    ///
    /// The action is responsible for transitioning the camera to an overview of the route.
    case showRouteOverview(() -> Void)
}

public extension CameraControlState {
    /// The recommended SF Symbol for the button state.
    var systemImageName: String {
        switch self {
        case .hidden, .showRecenter:
            "location.north.fill"
        case .showRouteOverview:
            "point.bottomleft.forward.to.point.topright.scurvepath"
        }
    }
}
