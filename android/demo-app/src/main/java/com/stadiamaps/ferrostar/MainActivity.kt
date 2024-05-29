package com.stadiamaps.ferrostar

import android.Manifest
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.mapboxsdk.geometry.LatLng
import com.maplibre.compose.symbols.Circle
import com.stadiamaps.ferrostar.core.AlternativeRouteProcessor
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.AndroidTtsStatusListener
import com.stadiamaps.ferrostar.core.CorrectiveAction
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.RouteDeviationHandler
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.ui.theme.FerrostarTheme
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.StepAdvanceMode
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

class MainActivity : ComponentActivity(), AndroidTtsStatusListener {
  companion object {
    private const val TAG = "MainActivity"
  }

  private val initialSimulatedLocation =
      UserLocation(
          GeographicCoordinate(37.807770999999995, -122.41970699999999),
          6.0,
          null,
          Instant.now(),
          null)
  private val locationProvider = SimulatedLocationProvider()
  private val httpClient = OkHttpClient.Builder().callTimeout(Duration.ofSeconds(15)).build()

  // NOTE: This is a public instance which is suitable for development, but not for heavy use.
  // This server is suitable for testing and building your app, but once you are ready to go live,
  // YOU MUST USE ANOTHER SERVER.
  //
  // See https://github.com/stadiamaps/ferrostar/blob/main/VENDORS.md for options
  private val core =
      FerrostarCore(
          valhallaEndpointURL = URL("https://valhalla1.openstreetmap.de/route"),
          profile = "bicycle",
          httpClient = httpClient,
          locationProvider = locationProvider,
          costingOptions = mapOf("bicycle" to mapOf("use_roads" to 0.2)))

  private lateinit var ttsObserver: AndroidTtsObserver

  override fun onDestroy() {
    super.onDestroy()

    // Don't forget to clean up!
    ttsObserver.shutdown()
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
    ttsObserver = AndroidTtsObserver(this, statusObserver = this)
    core.spokenInstructionObserver = ttsObserver

    // Not all navigation apps will require this sort of extra configuration.
    // In fact, we hope that most don't!
    // In case you do though, this sample implementation shows what you'll need to get started
    // (this basically re-implements the default behaviors).
    core.deviationHandler = RouteDeviationHandler { _, _, remainingWaypoints ->
      CorrectiveAction.GetNewRoutes(remainingWaypoints)
    }
    core.alternativeRouteProcessor = AlternativeRouteProcessor { core, routes ->
      if (routes.isNotEmpty()) {
        // NB: Use `replaceRoute` for cases like this!!
        core.replaceRoute(
            routes.first(),
            NavigationControllerConfig(
                StepAdvanceMode.RelativeLineStringDistance(
                    minimumHorizontalAccuracy = 25U, automaticAdvanceDistance = 10U),
                RouteDeviationTracking.StaticThreshold(25U, 10.0)))
      }
    }

    locationProvider.lastLocation = initialSimulatedLocation
    locationProvider.warpFactor = 2u

    setContent {
      var navigationViewModel by remember { mutableStateOf<NavigationViewModel?>(null) }

      // Get location permissions.
      // NOTE: This is NOT a robust suggestion for how to get permissions in a production app.
      // THis is simply minimal sample code in as few lines as possible.
      val locationPermissions =
          arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
      val permissionsLauncher =
          rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
              permissions ->
            when {
              permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // TODO
                // onAccess()
              }
              permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // TODO
                // onAccess()
              }
              else -> {
                // TODO
                // onFailed()
              }
            }
          }

      LaunchedEffect(savedInstanceState) {
        permissionsLauncher.launch(locationPermissions)
        // Fetch a route in the background
        launch(Dispatchers.IO) {
          val routes =
              core.getRoutes(
                  initialSimulatedLocation,
                  listOf(
                      Waypoint(
                          coordinate = GeographicCoordinate(37.807587, -122.428411),
                          kind = WaypointKind.BREAK),
                  ))

          val route = routes.first()
          navigationViewModel =
              core.startNavigation(
                  route = route,
                  config =
                      NavigationControllerConfig(
                          StepAdvanceMode.RelativeLineStringDistance(
                              minimumHorizontalAccuracy = 25U, automaticAdvanceDistance = 10U),
                          RouteDeviationTracking.StaticThreshold(25U, 10.0)),
              )

          locationProvider.setSimulatedRoute(route)
        }
      }

      FerrostarTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          val viewModel = navigationViewModel
          if (viewModel != null) {
            // Demo tiles illustrate a basic integration without any API key required,
            // but you can replace the styleURL with any valid MapLibre style URL.
            // See https://stadiamaps.github.io/ferrostar/vendors.html for some vendors.
            NavigationMapView(
                styleUrl = "https://demotiles.maplibre.org/style.json", viewModel = viewModel) {
                    uiState ->
                  // Trivial, if silly example of how to add your own overlay layers.
                  // (Also incidentally highlights the lag inherent in MapLibre location tracking
                  // as-is.)
                  Circle(
                      center =
                          LatLng(
                              uiState.value.snappedLocation.coordinates.lat,
                              uiState.value.snappedLocation.coordinates.lng),
                      radius = 10f,
                      color = "Blue",
                      zIndex = 2,
                  )
                }
          } else {
            // Loading indicator
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(text = "Calculating route...")
                  CircularProgressIndicator(modifier = Modifier.width(64.dp))
                }
          }
        }
      }
    }
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
