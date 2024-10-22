package com.stadiamaps.ferrostar

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.stadiamaps.ferrostar.core.AndroidTtsStatusListener
import com.stadiamaps.ferrostar.ui.theme.FerrostarTheme
import java.util.Locale

class MainActivity : ComponentActivity(), AndroidTtsStatusListener {
  companion object {
    private const val TAG = "MainActivity"
  }

  override fun onDestroy() {
    super.onDestroy()

    // Don't forget to clean up!
    AppModule.ttsObserver.shutdown()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Set up the App Module
    AppModule.init(this)

    // Set up text-to-speech for spoken instructions. This is a pretty "default" setup.
    // Most Android apps will want to set this up. TTS setup is *not* automatic.
    //
    // Be sure to read the class docs for further setup details.
    //
    // NOTE: We can't set this property in the same way as we do the core, because the context will
    // not be initialized yet, but the language won't save us from doing it anyways. This will
    // result in a confusing NPE.
    AppModule.ttsObserver.statusObserver = this
    AppModule.ferrostarCore.spokenInstructionObserver = AppModule.ttsObserver

    // Set up the location provider
    //    AppModule.locationProvider.lastLocation = initialSimulatedLocation
    //    AppModule.locationProvider.warpFactor = 2u

    // Edge to edge (this will be default in Android 15)
    // See https://developer.android.com/codelabs/edge-to-edge#0
    // How to: https://developer.android.com/develop/ui/compose/layouts/insets#insets-setup
    // IMPORTANT: If you use installSplashScreen(), you must call it before enableEdgeToEdge().
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }

    setContent {
      FerrostarTheme {
        // A surface container using the 'background' color from the theme
        Surface { DemoNavigationScene(savedInstanceState) }
      }
    }
  }

  // TTS listener methods

  override fun onTtsInitialized(tts: TextToSpeech?, status: Int) {
    // Set this up as appropriate for your app
    if (tts != null) {
      tts.setLanguage(Locale.US)
      android.util.Log.i(TAG, "setLanguage status: $status")
    } else {
      android.util.Log.e(TAG, "TTS setup failed! $status")
    }
  }

  override fun onTtsSpeakError(utteranceId: String, status: Int) {
    android.util.Log.e(
        TAG, "Something went wrong synthesizing utterance $utteranceId. Status code: $status.")
  }
}
