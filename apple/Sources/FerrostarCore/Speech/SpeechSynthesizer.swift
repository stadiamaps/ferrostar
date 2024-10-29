import AVFoundation
import Foundation

/// An abstracted speech synthesizer that is used by the ``SpokenInstructionObserver``
///
/// Any functions that are needed for use in the ``SpokenInstructionObserver`` should be exposed through
/// this protocol.
public protocol SpeechSynthesizer {
    // TODO: We could further abstract this to allow other speech synths.
    //       E.g. with a `struct SpeechUtterance` if and when another speech service comes along.

    var isSpeaking: Bool { get }
    func speak(_ utterance: AVSpeechUtterance)
    @discardableResult
    func stopSpeaking(at boundary: AVSpeechBoundary) -> Bool
}

extension AVSpeechSynthesizer: SpeechSynthesizer {
    // No def required
}

class PreviewSpeechSynthesizer: SpeechSynthesizer {
    public var isSpeaking: Bool = false

    public func speak(_: AVSpeechUtterance) {
        // No action for previews
    }

    public func stopSpeaking(at _: AVSpeechBoundary) -> Bool {
        // No action for previews
        true
    }
}
