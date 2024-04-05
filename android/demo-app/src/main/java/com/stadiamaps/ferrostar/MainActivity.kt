package com.stadiamaps.ferrostar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.stadiamaps.ferrostar.core.AlternativeRouteProcessor
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

class MainActivity : ComponentActivity() {
  private val initialSimulatedLocation =
      UserLocation(
          GeographicCoordinate(37.807770999999995, -122.41970699999999), 6.0, null, Instant.now())
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
      )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Not all navigation apps will require this sort of extra configuration.
    // In fact, we hope that most don't!
    // In case you do though, this sample implementation shows what you'll need to get started
    // (this basically re-implements the default behaviors).
    core.deviationHandler = RouteDeviationHandler { _, _, remainingWaypoints ->
      CorrectiveAction.GetNewRoutes(remainingWaypoints)
    }
    core.alternativeRouteProcessor = AlternativeRouteProcessor { core, routes ->
      if (routes.isNotEmpty()) {
        core.startNavigation(
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

      LaunchedEffect(savedInstanceState) {
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
                styleUrl = "https://demotiles.maplibre.org/style.json", viewModel = viewModel)
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
}
