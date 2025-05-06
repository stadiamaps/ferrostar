import FerrostarCore
import SwiftUI

public struct NavigationUIMuteButton: View {
    let isMuted: Bool
    let action: () -> Void

    public init(isMuted: Bool, action: @escaping () -> Void) {
        self.isMuted = isMuted
        self.action = action
    }

    public var body: some View {
        NavigationUIButton(action: action) {
            Image(systemName: isMuted ? "speaker.slash.fill" : "speaker.2.fill")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 18, height: 18)
        }
    }
}

#Preview {
    VStack {
        NavigationUIMuteButton(isMuted: true, action: {})

        NavigationUIMuteButton(isMuted: false, action: {})
    }
}
