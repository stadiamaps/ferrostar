import AVFoundation
import Combine
import FerrostarCoreFFI
import Foundation

/// An Spoken instruction provider that triggers speech synthesis in response to navigation events.
///
/// Automatically handles audio session management,
/// including ducking volume from other apps when appropriate.
// @unchecked Sendable safety: mutable state is isolated to @MainActor.
// The @Published wrapper is not Sendable but access is protected by MainActor isolation.
public final class SpokenInstructionObserver: @unchecked Sendable {
    @MainActor @Published public private(set) var isMuted: Bool

    let synthesizer: any SpeechSynthesizer
    private let audioManager = AudioSessionManager()
    @MainActor private var audioFocusReleaseTask: Task<Void, Never>?

    /// Creates a spoken instruction observer with any ``SpeechSynthesizer``.
    ///
    /// - Parameters:
    ///   - synthesizer: The speech synthesizer.
    ///   - isMuted: Whether the speech synthesizer is currently muted. Assume false if unknown.
    @MainActor
    public init(
        synthesizer: any SpeechSynthesizer,
        isMuted: Bool
    ) {
        self.synthesizer = synthesizer
        self.isMuted = isMuted
    }

    public func spokenInstructionTriggered(_ instruction: FerrostarCoreFFI.SpokenInstruction) {
        Task {
            let muted = await isMuted
            guard !muted else {
                return
            }

            await cancelAudioFocusRelease()
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
            await scheduleAudioFocusRelease()
        }
    }

    /// Toggle the mute.
    @MainActor
    public func toggleMute() {
        let isCurrentlyMuted = isMuted
        isMuted = !isCurrentlyMuted

        if isMuted {
            stopAndClearQueue()
        }
    }

    @MainActor
    public func stopAndClearQueue() {
        Task {
            synthesizer.stopSpeaking(at: .immediate)
            await audioManager.releaseAudioFocus()
        }
    }

    @MainActor
    func scheduleAudioFocusRelease() {
        cancelAudioFocusRelease()

        audioFocusReleaseTask = Task { [weak self] in
            guard let self else { return }
            repeat {
                try? await Task.sleep(nanoseconds: 500 * NSEC_PER_MSEC)
            } while !Task.isCancelled && self.synthesizer.isSpeaking

            guard !Task.isCancelled else {
                return
            }

            await self.audioManager.releaseAudioFocus()
        }
    }

    @MainActor
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
    @MainActor
    static func initAVSpeechSynthesizer(synthesizer: AVSpeechSynthesizer = AVSpeechSynthesizer(),
                                        isMuted: Bool = false) -> SpokenInstructionObserver
    {
        SpokenInstructionObserver(synthesizer: synthesizer, isMuted: isMuted)
    }
}
