import AVFoundation
import Combine
import FerrostarCoreFFI
import Foundation

/// An observer for spoken instruction events.
///
/// Both a ``DummyInstructionObserver`` and ``AVSpeechSpokenInstructionObserver``
/// implementation are provided,
/// but you can also swap your own for a proprietary service.
public protocol SpokenInstructionObserver {
    /// Handles spoken instructions as they are triggered.
    ///
    /// As long as it is used with the supplied ``FerrostarCore`` class,
    /// implementors may assume this function will never be called twice
    /// for the same instruction during a navigation session.
    func spokenInstructionTriggered(_ instruction: SpokenInstruction)

    /// Stops speech and clears the queue of spoken utterances.
    func stopAndClearQueue()

    var isMuted: Bool { get }

    /// Mute or unmute the text-to-speech speech engine.
    ///
    /// - Parameter muted: Mute the text-to-speech engine if true, unmute if false.
    func setMuted(_ muted: Bool)
}

public class DummyInstructionObserver: SpokenInstructionObserver, ObservableObject {
    @Published public private(set) var isMuted: Bool = false

    public init(isMuted: Bool = false) {
        self.isMuted = isMuted
    }

    public func setMuted(_ muted: Bool) {
        isMuted = muted
    }

    public func spokenInstructionTriggered(_: SpokenInstruction) {
        // Do nothing
    }

    public func stopAndClearQueue() {
        // Do nothing
    }
}

/// Speech synthesis backed by `AVSpeechSynthesizer`.
public class AVSpeechSpokenInstructionObserver: SpokenInstructionObserver, ObservableObject {
    @Published public private(set) var isMuted: Bool

    public let synthesizer = AVSpeechSynthesizer()

    public init(isMuted: Bool) {
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

    public func setMuted(_ muted: Bool) {
        isMuted = muted

        if muted, synthesizer.isSpeaking {
            stopAndClearQueue()
        }
    }

    public func stopAndClearQueue() {
        synthesizer.stopSpeaking(at: .immediate)
    }
}
