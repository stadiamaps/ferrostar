import FerrostarCore
import SwiftUI

public struct NavigationUICameraButton: View {
    let state: CameraControlState

    /// Create a Camera Centering Button
    ///
    /// - Parameters:
    ///   - state: The current state of the camera control.
    public init(state: CameraControlState) {
        self.state = state
    }

    public var body: some View {
        switch state {
        case .hidden:
            EmptyView()
        case let .showRecenter(action),
             let .showCurrentLocation(action),
             let .showRouteOverview(action):
            NavigationUIButton(action: action) {
                Image(systemName: state.systemImageName)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 18, height: 18)
            }
            .accessibilityLabel(cameraAccessibilityLabel)
        }
    }

    private var cameraAccessibilityLabel: Text {
        switch state {
        case .hidden, .showRecenter:
            Text("Recenter map", bundle: .module,
                 comment: "Accessibility label for the button that returns the camera to following the user.")
        case .showCurrentLocation:
            Text("Show current location", bundle: .module,
                 comment: "Accessibility label for the button that returns from route overview to the user's location.")
        case .showRouteOverview:
            Text("Show route overview", bundle: .module,
                 comment: "Accessibility label for the button that displays the entire navigation route.")
        }
    }
}

#Preview {
    VStack {
        NavigationUICameraButton(state: .showRecenter {})
    }
}
