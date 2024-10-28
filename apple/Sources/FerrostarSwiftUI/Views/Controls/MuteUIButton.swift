import FerrostarCore
import FerrostarCoreFFI
import SwiftUI

public struct MuteUIButton<T: SpokenInstructionObserver & ObservableObject>: View {
    @ObservedObject var spokenInstructionObserver: T

    public var body: some View {
        Button(action: {
            spokenInstructionObserver.setMuted(!spokenInstructionObserver.isMuted)
        }) {
            Image(systemName: spokenInstructionObserver.isMuted ? "speaker.slash.fill" : "speaker.2.fill")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 18, height: 18)
                .padding()
        }
        .foregroundColor(.black)
        .background(Color.white)
        .clipShape(Circle())
        .shadow(radius: 10)
    }
}

#Preview {
    MuteUIButton(spokenInstructionObserver: DummyInstructionObserver())
}
