import AVFoundation
import FerrostarCoreFFI
import Foundation

public protocol SpokenInstructionObserver {
    /// Mute or unmute the speech engine.
    ///
    /// - Parameter mute: Mute the speech engine if true, unmute if false.
    func setMute(_ mute: Bool)

    /// Handles spoken instructions as they are triggered.
    ///
    /// As long as it is used with the supplied ``FerrostarCore`` class,
    /// implementors may assume this function will never be called twice
    /// for the same instruction during a navigation session.
    func spokenInstructionTriggered(_ instruction: SpokenInstruction)

    /// Stops speech and clears the queue of spoken utterances.
    func stopAndClearQueue()

    /// If the speech synthisizer is currently muted.
    var isMuted: Bool { get }
}

public class AVSpeechSpokenInstructionObserver: ObservableObject, SpokenInstructionObserver {
    @Published public private(set) var isMuted: Bool

    private let synthesizer = AVSpeechSynthesizer()

    public init(isMuted: Bool) {
        self.isMuted = isMuted
    }

    public func setMute(_ mute: Bool) {
        guard isMuted != mute else {
            return
        }

        // Immediately set the publisher. This will avoid delays updating UI.
        isMuted = mute

        if mute, synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
    }

    public func spokenInstructionTriggered(_ instruction: FerrostarCoreFFI.SpokenInstruction) {
        guard !isMuted else {
            return
        }

        let utterance: AVSpeechUtterance = if #available(iOS 16.0, *),
                                              let ssml = instruction.ssml,
                                              let ssmlUtterance = AVSpeechUtterance(ssmlRepresentation: ssml)
        {
            ssmlUtterance
        } else {
            AVSpeechUtterance(string: instruction.text)
        }

        synthesizer.speak(utterance)
    }

    public func stopAndClearQueue() {
        synthesizer.stopSpeaking(at: .immediate)
    }
}
