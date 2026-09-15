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

    /// Whether this observer should take over the app's `AVAudioSession` while speaking.
    ///
    /// Defaults to `true`, which preserves the existing behavior: audio focus is requested
    /// (ducking other apps) before speaking and released afterwards.
    ///
    /// Set this to `false` when the host app manages its own audio session. This matters for apps
    /// that inject a custom ``SpeechSynthesizer`` which plays audio through the app's own session:
    ///
    /// * `requestAudioFocus()` sets `.duckOthers` and `.voicePrompt` on the shared session.
    /// * `releaseAudioFocus()` only clears `hasAudioFocus` **after** `setActive(false)` succeeds,
    ///   and `setActive(false)` fails while the session still has active audio I/O.
    ///
    /// An app that keeps a microphone tap open (e.g. for wake-word standby) or plays its own
    /// audio therefore never releases focus, so other apps stay ducked for the rest of the
    /// session. Recovering by re-applying `setCategory` is not a workable fix either: changing
    /// Whether this observer should take over the app's `AVAudioSession` while speaking.
    ///
    /// When true, automatically manages audio focus (ducking other apps)
    /// before speaking, and releases after each instruction.
    ///
    /// Setting it to `false` means the application will manage this itself.
    /// This matters for some apps that inject a custom ``SpeechSynthesizer``
    /// which plays audio through the app's own session.
    private let managesAudioSession: Bool

    /// Creates a spoken instruction observer with any ``SpeechSynthesizer``.
    ///
    /// - Parameters:
    ///   - synthesizer: The speech synthesizer.
    ///   - isMuted: Whether the speech synthesizer is currently muted. Assume false if unknown.
    /// - Parameters:
    ///   - synthesizer: The speech synthesizer.
    ///   - isMuted: Whether the speech synthesizer is currently muted. (Normally this will be false,
    ///     unless you're providing your own "hot" synth.)
    ///   - managesAudioSession: Whether this observer should manage the shared `AVAudioSession`
    ///     while speaking.
    ///     Set to `false` if the host app will manage the audio session lifecycle and focus itself.
    public init(
        synthesizer: SpeechSynthesizer,
        isMuted: Bool,
        managesAudioSession: Bool = true
    ) {
        self.synthesizer = synthesizer
        self.isMuted = isMuted
        self.managesAudioSession = managesAudioSession
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
            if managesAudioSession {
                await audioManager.requestAudioFocus()
            }

            let utterance: AVSpeechUtterance = if #available(iOS 16.0, *),
                                                  let ssml = instruction.ssml,
                                                  let ssmlUtterance = AVSpeechUtterance(ssmlRepresentation: ssml)
            {
                ssmlUtterance
            } else {
                AVSpeechUtterance(string: instruction.text)
            }

            self.synthesizer.speak(utterance)
            if managesAudioSession {
                scheduleAudioFocusRelease()
            }
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
            if managesAudioSession {
                await audioManager.releaseAudioFocus()
            }
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
