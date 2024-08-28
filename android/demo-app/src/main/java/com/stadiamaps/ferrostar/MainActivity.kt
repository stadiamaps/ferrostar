package com.stadiamaps.ferrostar

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ferrostar.carui.FerrostarCarAppManager
import com.stadiamaps.ferrostar.core.AndroidTtsStatusListener
import com.stadiamaps.ferrostar.support.initialSimulatedLocation
import com.stadiamaps.ferrostar.ui.theme.FerrostarTheme
import java.util.Locale

class MainActivity : ComponentActivity(), AndroidTtsStatusListener {
  companion object {
    private const val TAG = "MainActivity"
  }

  private val appModule: AppModule
    get() = (application as DemoApplication).appModule

  override fun onDestroy() {
    super.onDestroy()

    // Don't forget to clean up!
    appModule.ttsObserver.shutdown()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Set up text-to-speech for spoken instructions. This is a pretty "default" setup.
    // Most Android apps will want to set this up. TTS setup is *not* automatic.
    //
    // Be sure to read the class docs for further setup details.
    //
    // NOTE: We can't set this property in the same way as we do the core, because the context will
    // not be initialized yet, but the language won't save us from doing it anyways. This will
    // result in a confusing NPE.
    appModule.ttsObserver.statusObserver = this
    appModule.ferrostarCore.spokenInstructionObserver = appModule.ttsObserver

    // Set up the location provider
    appModule.locationProvider.lastLocation = initialSimulatedLocation
    appModule.locationProvider.warpFactor = 2u

    setContent {
      FerrostarTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          DemoNavigationScene(
              savedInstanceState, appModule.navigationViewModel, appModule.locationProvider)
        }
      }
    }

    // TODO: This is pretty flimsy. Just testing it out.
    val ferrostarCarAppService = FerrostarCarAppManager(this)
    ferrostarCarAppService.start(this)
  }

  // TTS listener methods

  override fun onTtsInitialized(tts: TextToSpeech?, status: Int) {
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
