import SwiftUI

public struct MuteUIButton: View {
    @Binding public var isMuted: Bool

    public init(isMuted: Binding<Bool>) {
        self._isMuted = isMuted
    }

    public var body: some View {
        Button(action: {
            isMuted.toggle()
        }) {
            Image(systemName: isMuted ? "speaker.slash.fill" : "speaker.2.fill")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 18, height: 18)
                .padding()
                .foregroundColor(.black)
                .background(Color.white)
                .clipShape(Circle())
                .shadow(radius: 10)
        }
    }
}

#Preview {
    MuteUIButton(isMuted: .constant(false))
}
