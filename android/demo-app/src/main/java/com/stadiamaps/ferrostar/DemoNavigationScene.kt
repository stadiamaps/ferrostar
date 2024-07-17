package com.stadiamaps.ferrostar

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.maplibreui.views.DynamicallyOrientingNavigationView
import com.stadiamaps.ferrostar.support.initialSimulatedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.StepAdvanceMode
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

@Composable
fun DemoNavigationScene(
    savedInstanceState: Bundle?,
    locationProvider: SimulatedLocationProvider = AppModule.locationProvider,
    core: FerrostarCore = AppModule.ferrostarCore
) {
  var viewModel by remember { mutableStateOf<NavigationViewModel?>(null) }

  // Get location permissions.
  // NOTE: This is NOT a robust suggestion for how to get permissions in a production app.
  // THis is simply minimal sample code in as few lines as possible.
  val locationPermissions =
      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
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
      viewModel =
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

  if (viewModel != null) {
    // Demo tiles illustrate a basic integration without any API key required,
    // but you can replace the styleURL with any valid MapLibre style URL.
    // See https://stadiamaps.github.io/ferrostar/vendors.html for some vendors.
    DynamicallyOrientingNavigationView(
        modifier = Modifier.fillMaxSize(),
        styleUrl = "https://demotiles.maplibre.org/style.json",
        viewModel = viewModel!!) { uiState ->
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
