import SwiftUI

struct MuteButton: View {
    @Binding var isMuted: Bool

    var body: some View {
        Button(action: {
            isMuted.toggle()
        }) {
            Image(systemName: isMuted ? "speaker.slash.fill" : "speaker.2.fill")
                .resizable()
                .aspectRatio(contentMode: .fit)
        }
    }
}
