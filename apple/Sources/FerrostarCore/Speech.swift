import AVFoundation
import FerrostarCoreFFI
import Foundation

public protocol SpokenInstructionObserver {
    /// Handles spoken instructions as they are triggered.
    ///
    /// As long as it is used with the supplied ``FerrostarCore`` class,
    /// implementors may assume this function will never be called twice
    /// for the same instruction during a navigation session.
    func spokenInstructionTriggered(_ instruction: SpokenInstruction)

    var isMuted: Bool { get set }
}

public class AVSpeechSpokenInstructionObserver: SpokenInstructionObserver {
    public var isMuted: Bool {
        didSet {
            if isMuted, synthesizer.isSpeaking {
                synthesizer.stopSpeaking(at: .immediate)
            }
        }
    }
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
}
