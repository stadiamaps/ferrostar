package com.stadiamaps.ferrostar.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
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

  var isMuted: Boolean
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
    engine: String? = null,
    var statusObserver: AndroidTtsStatusListener? = null,
) : SpokenInstructionObserver, OnInitListener {
  companion object {
    private const val TAG = "AndroidTtsObserver"
  }

  override var isMuted: Boolean = false
    set(value) {
      field = value

      if (value && tts?.isSpeaking == true) {
        tts?.stop()
      }
    }

  var tts: TextToSpeech?
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

  init {
    tts = TextToSpeech(context, this, engine)
  }

  /**
   * Speaks the provided instruction.
   *
   * Fails silently if TTS is unavailable.
   */
  override fun onSpokenInstructionTrigger(spokenInstruction: SpokenInstruction) {
    val tts = tts
    if (tts != null && isInitializedSuccessfully && !isMuted) {
      // In the future, someone may wish to parse SSML to get more natural utterances into TtsSpans.
      // Amazon Polly is generally the intended target for SSML on Android though.
      val status =
          tts.speak(
              spokenInstruction.text, TextToSpeech.QUEUE_ADD, null, spokenInstruction.utteranceId)
      if (status != TextToSpeech.SUCCESS) {
        android.util.Log.e(TAG, "Unable to speak instruction: code $status")
        statusObserver?.onTtsSpeakError(spokenInstruction.utteranceId, status)
      }
    }
  }

  override fun onInit(status: Int) {
    initStatus = status
    if (status != TextToSpeech.SUCCESS) {
      tts = null
      android.util.Log.e(TAG, "Unable to initialize TTS engine: code $status")
    }

    statusObserver?.onTtsInitialized(tts, status)
  }

  override fun stopAndClearQueue() {
    tts?.stop()
  }

  /**
   * Shuts down the underlying [TextToSpeech] engine.
   *
   * The instance will no longer be usable after this. This method should usually be called from an
   * activity's onDestroy method.
   */
  fun shutdown() {
    tts?.shutdown()
    tts = null
  }
}
