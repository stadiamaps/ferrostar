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

  var isMuted: Boolean
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
 * @param configure a callback will to be invoked once the engine either succeeds or fails to
 *   initialize. You can use this to configure the instance (ex: by calling
 *   [TextToSpeech.setLanguage]). If initialization failed, the argument to the callback will be
 *   null.
 */
class AndroidTtsObserver(
    context: Context,
    engine: String? = null,
    private val configure: (TextToSpeech?) -> Unit = {}
) : SpokenInstructionObserver, OnInitListener {
  companion object {
    private const val TAG = "AndroidTtsObserver"
  }

  override var isMuted: Boolean = false
    set(value) {
      field = value

      if (tts?.isSpeaking == true) {
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
    if (isInitializedSuccessfully && !isMuted) {
      // In the future, someone may wish to parse SSML to get more natural utterances into TtsSpans.
      // Amazon Polly is generally the intended target for SSML on Android though.
      val status =
          tts?.speak(
              spokenInstruction.text, TextToSpeech.QUEUE_ADD, null, spokenInstruction.utteranceId)
      if (status != TextToSpeech.SUCCESS) {
        android.util.Log.e(TAG, "Unable to speak instruction (result was not SUCCESS).")
      }
    }
  }

  override fun onInit(status: Int) {
    initStatus = status
    if (status != TextToSpeech.SUCCESS) {
      tts = null
      android.util.Log.e(TAG, "Unable to initialize TTS engine: code $status")
    }

    configure(tts)
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
