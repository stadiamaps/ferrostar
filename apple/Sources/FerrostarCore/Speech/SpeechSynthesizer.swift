import AVFoundation
import Foundation

/// An abstracted speech synthesizer that is used by the ``SpokenInstructionObserver``
///
/// Any functions that are needed for use in the ``SpokenInstructionObserver`` should be exposed through
/// this protocol.
public protocol SpeechSynthesizer: Sendable {
    // TODO: We could further abstract this to allow other speech synths.
    //       E.g. with a `struct SpeechUtterance` if and when another speech service comes along.

    var isSpeaking: Bool { get }
    func speak(_ utterance: AVSpeechUtterance)
    @discardableResult
    func stopSpeaking(at boundary: AVSpeechBoundary) -> Bool
}

extension AVSpeechSynthesizer: @unchecked Sendable, SpeechSynthesizer {
    // AVSpeechSynthesizer is thread-safe in practice.
}

final class PreviewSpeechSynthesizer: SpeechSynthesizer, @unchecked Sendable {
    var isSpeaking: Bool = false

    func speak(_: AVSpeechUtterance) {
        // No action for previews
    }

    func stopSpeaking(at _: AVSpeechBoundary) -> Bool {
        // No action for previews
        true
    }
}
