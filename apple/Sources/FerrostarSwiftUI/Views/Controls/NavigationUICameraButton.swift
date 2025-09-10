import FerrostarCore
import SwiftUI

public struct NavigationUICameraButton: View {
    let state: CameraControlState

    /// <#Description#>
    ///
    /// - Parameters:
    ///   - state: <#state description#>
    public init(state: CameraControlState) {
        self.state = state
    }

    public var body: some View {
        switch state {
        case .hidden:
            EmptyView()
        case let .showRecenter(action),
             let .showRouteOverview(action):
            NavigationUIButton(action: action) {
                Image(systemName: state.systemImageName)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 18, height: 18)
            }
        }
    }
}

#Preview {
    VStack {
        NavigationUICameraButton(state: .showRecenter {})
    }
}
