import AVFoundation
import Combine
import FerrostarCoreFFI
import Foundation

/// An Spoken instruction provider that takes a speech synthesizer.
public class SpokenInstructionObserver: ObservableObject {
    @Published public private(set) var isMuted: Bool

    private let synthesizer: SpeechSynthesizer

    /// Create a spoken instruction observer with any ``SpeechSynthesizer``
    ///
    /// - Parameters:
    ///   - synthesizer: The speech synthesizer.
    ///   - isMuted: Whether the speech synthesizer is currently muted. Assume false if unknown.
    public init(
        synthesizer: SpeechSynthesizer,
        isMuted: Bool
    ) {
        self.synthesizer = synthesizer
        self.isMuted = isMuted
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

    /// Toggle the mute.
    public func toggleMute() {
        // TODO: We flip the publisher before actually stopping the synthesizer for a responsive.
        //      But this still seems a little jumpy/slow. We may want to use some Tasks/concurrency
        //      to separate UI on main, and speech on another queue.
        let isCurrentlyMuted = isMuted
        isMuted = !isCurrentlyMuted

        // This used to have `synthesizer.isSpeaking`, but I think we want to run it regardless.
        if isMuted {
            stopAndClearQueue()
        }
    }

    public func stopAndClearQueue() {
        synthesizer.stopSpeaking(at: .immediate)
    }
}

public extension SpokenInstructionObserver {
    /// Create a new spoken instruction observer with AFFoundation's AVSpeechSynthesizer.
    ///
    /// - Parameters:
    ///    - synthesizer: An instance of AVSpeechSynthesizer. One is provided by default, but you can inject your own.
    ///    - isMuted: If the synthesizer is muted. This should be false unless you're providing a "hot" synth that is
    /// speaking.
    /// - Returns: The instance of SpokenInstructionObserver
    static func initAVSpeechSynthesizer(synthesizer: AVSpeechSynthesizer = AVSpeechSynthesizer(),
                                        isMuted: Bool = false) -> SpokenInstructionObserver
    {
        SpokenInstructionObserver(synthesizer: synthesizer, isMuted: isMuted)
    }
}
