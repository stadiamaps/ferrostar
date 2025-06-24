package com.stadiamaps.ferrostar.core

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.VisibleForTesting
import java.lang.ref.WeakReference
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uniffi.ferrostar.SpokenInstruction

interface SpokenInstructionObserver {

  /**
   * Handles spoken instructions as they are triggered.
   *
   * As long as it is used with the supplied [FerrostarCore] class, implementors may assume this
   * function will never be called twice for the same instruction during a navigation session.
   */
  fun onSpokenInstructionTrigger(spokenInstruction: SpokenInstruction)

  /** Stops speech and clears the queue of spoken utterances. */
  fun stopAndClearQueue()

  fun setMuted(isMuted: Boolean)

  val muteState: StateFlow<Boolean>

  val isMuted: Boolean
    get() = muteState.value
}

/** Observes the status of an [AndroidTtsObserver]. */
interface AndroidTtsStatusListener {
  /**
   * Invoked when the [TextToSpeech] instance initialization is complete.
   *
   * If successful, the value [tts] will be non-null. Otherwise, it will be null. The status code
   * returned by the init listener is passed as-is via [status].
   */
  fun onTtsInitialized(tts: TextToSpeech?, status: Int)

  /**
   * Invoked when the [TextToSpeech] instance is shut down and released to nil.
   *
   * After this point you must initialize a new instance to use TTS by calling
   * [AndroidTtsObserver.start].
   */
  fun onTtsShutdownAndRelease()

  /**
   * Invoked whenever [TextToSpeech.speak] returns a status code other than [TextToSpeech.SUCCESS].
   */
  fun onTtsSpeakError(utteranceId: String, status: Int)
}

/**
 * A configurable [SpokenInstructionObserver] integrated with the built-in Android text-to-speech
 * APIs.
 *
 * The constructor will attempt to initialize a [TextToSpeech] instance automatically. If
 * successful, the various status properties will be set. As this may not initialize
 * instantaneously, you should create an instance of this class before starting navigation. This
 * should generally be created and destroyed in your activity lifecycle methods.
 *
 * When you are done with the instance (ex: in your activity onDestroy method), call [shutdown] to
 * properly clean up resources.
 *
 * @param context the Android context (required to initialize the [TextToSpeech] class).
 * @param engine the text to speech engine package name. If null, the system default is used.
 * @param statusObserver an object that listens for status events like initialization and synthesis
 *   errors.
 */
class AndroidTtsObserver(
    context: Context,
    private val weakContext: WeakReference<Context> = WeakReference(context),
    private val engine: String? = null,
    var statusObserver: AndroidTtsStatusListener? = null,
) : SpokenInstructionObserver, OnInitListener {
  companion object {
    private const val TAG = "AndroidTtsObserver"
  }

  private var _muteState: MutableStateFlow<Boolean> = MutableStateFlow(false)

  private val context: Context?
    get() = weakContext.get()

  private val audioFocusManager =
      AudioFocusManager(
          context,
          AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
              AudioManager.AUDIOFOCUS_LOSS,
              AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Handle focus loss if needed (pause TTS)
                stopAndClearQueue()
              }
              AudioManager.AUDIOFOCUS_GAIN -> {
                // Audio focus regained; no action needed
              }
            }
          })

  override fun setMuted(isMuted: Boolean) {
    _muteState.update { _ ->
      if (isMuted && tts?.isSpeaking == true) {
        tts?.stop()
      }
      isMuted
    }
  }

  override val muteState: StateFlow<Boolean> = _muteState.asStateFlow()

  var tts: TextToSpeech? = null
    private set

  /**
   * The initialization status.
   *
   * On success, this will be [TextToSpeech.SUCCESS]. For other error codes, refer to the
   * [TextToSpeech] docs. See the docs for instructions on how to set up your manifest to enable
   * TTS.
   *
   * The [isInitializedSuccessfully] property provides a more convenient way to determine if the
   * engine is ready. This exposes the raw code in case you need it in your app.
   */
  var initStatus: Int? = null
    private set

  /** True if [tts] is initialized successfully. */
  val isInitializedSuccessfully: Boolean
    get() = initStatus == TextToSpeech.SUCCESS

  /**
   * Starts a new [TextToSpeech] instance.
   *
   * Except [onInit] to fire when the engine is ready.
   */
  fun start(@VisibleForTesting injectedTts: TextToSpeech? = null) {
    if (context == null) {
      android.util.Log.e(TAG, "Context is null. Unable to start TTS.")
      return
    }

    if (tts != null) {
      android.util.Log.e(TAG, "TTS engine is already initialized.")
      return
    }

    tts = injectedTts ?: TextToSpeech(context, this, engine)
  }

  /**
   * Speaks the provided instruction.
   *
   * Fails silently if TTS is unavailable.
   */
  override fun onSpokenInstructionTrigger(spokenInstruction: SpokenInstruction) {
    if (tts == null || !isInitializedSuccessfully) {
      android.util.Log.e(TAG, "TTS engine is not initialized.")
      return
    }
    val tts = tts ?: return

    if (!audioFocusManager.requestAudioFocus()) {
      android.util.Log.w(TAG, "Unable to request audio focus; TTS will mix with audio from other apps.")
    }

    if (!isMuted) {
      // In the future, someone may wish to parse SSML to get more natural utterances into TtsSpans.
      // Amazon Polly is generally the intended target for SSML on Android though.
      val status =
          tts.speak(
              spokenInstruction.text,
              TextToSpeech.QUEUE_ADD,
              null,
              spokenInstruction.utteranceId.toString())
      if (status != TextToSpeech.SUCCESS) {
        android.util.Log.e(TAG, "Unable to speak instruction: code $status")
        statusObserver?.onTtsSpeakError(spokenInstruction.utteranceId.toString(), status)
      }
    }
  }

  override fun onInit(status: Int) {
    initStatus = status
    if (status != TextToSpeech.SUCCESS) {
      tts = null
      android.util.Log.e(TAG, "Unable to initialize TTS engine: code $status")
    } else {
      tts?.setOnUtteranceProgressListener(utteranceProgressListener)
    }

    statusObserver?.onTtsInitialized(tts, status)
  }

  override fun stopAndClearQueue() {
    tts?.stop()
    audioFocusManager.releaseAudioFocus()
  }

  /**
   * Shuts down the underlying [TextToSpeech] engine.
   *
   * The instance will be shut down and released after this call. You must call [start] to use TTS
   * again. If you call this method, ensure that you've handled an start again.
   */
  fun shutdown() {
    tts?.shutdown()
    tts = null
    statusObserver?.onTtsShutdownAndRelease()
    audioFocusManager.releaseAudioFocus()
  }

  // Audio session ducking
  private var releaseTimer: Timer? = null

  // Create the listener as a property
  private val utteranceProgressListener =
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          releaseTimer?.cancel()
          releaseTimer = null
        }

        override fun onDone(utteranceId: String?) {
          scheduleAudioFocusRelease()
        }

        override fun onError(utteranceId: String?) {
          audioFocusManager.releaseAudioFocus()
        }
      }

  private val RELEASE_DELAY_MS = 500L // 500ms after last utterance

  private fun scheduleAudioFocusRelease() {
    releaseTimer?.cancel()
    releaseTimer =
        Timer().apply {
          schedule(
              object : TimerTask() {
                override fun run() {
                  audioFocusManager.releaseAudioFocus()
                }
              },
              RELEASE_DELAY_MS)
        }
  }
}
