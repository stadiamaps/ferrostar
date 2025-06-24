import AVFoundation
import Combine
import FerrostarCoreFFI
import Foundation

/// An Spoken instruction provider that triggers speech synthesis in response to navigation events.
///
/// Automatically handles audio session management,
/// including ducking volume from other apps when appropriate.
public class SpokenInstructionObserver {
    @Published public private(set) var isMuted: Bool

    let synthesizer: SpeechSynthesizer
    private let audioManager = AudioSessionManager()
    private var audioFocusReleaseTask: Task<Void, Never>?

    /// Creates a spoken instruction observer with any ``SpeechSynthesizer``.
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

    deinit {
        audioFocusReleaseTask?.cancel()
        // NOTE: The audioFocusReleaseTask will deinit itself
    }

    public func spokenInstructionTriggered(_ instruction: FerrostarCoreFFI.SpokenInstruction) {
        guard !isMuted else {
            return
        }

        Task {
            cancelAudioFocusRelease()
            await audioManager.requestAudioFocus()

            let utterance: AVSpeechUtterance = if #available(iOS 16.0, *),
                                                  let ssml = instruction.ssml,
                                                  let ssmlUtterance = AVSpeechUtterance(ssmlRepresentation: ssml)
            {
                ssmlUtterance
            } else {
                AVSpeechUtterance(string: instruction.text)
            }

            self.synthesizer.speak(utterance)
            scheduleAudioFocusRelease()
        }
    }

    /// Toggle the mute.
    public func toggleMute() {
        let isCurrentlyMuted = isMuted
        isMuted = !isCurrentlyMuted

        // This used to have `synthesizer.isSpeaking`, but I think we want to run it regardless.
        if isMuted {
            stopAndClearQueue()
        }
    }

    public func stopAndClearQueue() {
        Task {
            synthesizer.stopSpeaking(at: .immediate)
            await audioManager.releaseAudioFocus()
        }
    }

    func scheduleAudioFocusRelease() {
        cancelAudioFocusRelease()

        audioFocusReleaseTask = Task { [weak self] in
            // Wait at least 500ms; then keep waiting until either:
            //   - The task is cancelled
            //   - The synthesizer is no longer speaking
            repeat {
                try? await Task.sleep(nanoseconds: 500 * NSEC_PER_MSEC)
            } while !Task.isCancelled && self?.synthesizer.isSpeaking ?? false

            guard !Task.isCancelled else {
                return
            }

            // Release audio focus (unduck other sources) once we're done speaking
            await self?.audioManager.releaseAudioFocus()
        }
    }

    private func cancelAudioFocusRelease() {
        audioFocusReleaseTask?.cancel()
        audioFocusReleaseTask = nil
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
