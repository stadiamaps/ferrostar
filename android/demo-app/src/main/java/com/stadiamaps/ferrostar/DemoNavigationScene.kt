package com.stadiamaps.ferrostar

import android.Manifest
import android.os.Build
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
import com.stadiamaps.ferrostar.composeui.runtime.KeepScreenOnDisposableEffect
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.maplibreui.views.DynamicallyOrientingNavigationView
import com.stadiamaps.ferrostar.support.initialSimulatedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

@Composable
fun DemoNavigationScene(
    savedInstanceState: Bundle?,
    locationProvider: SimulatedLocationProvider = AppModule.locationProvider,
    core: FerrostarCore = AppModule.ferrostarCore
) {
  // Keeps the screen at consistent brightness on while this Composable is in the view hierarchy.
  // This is typically a good idea for scenes hosting navigation trips or a NavigationView.
  KeepScreenOnDisposableEffect()

  var viewModel by remember { mutableStateOf<NavigationViewModel?>(null) }

  // Get location permissions.
  // NOTE: This is NOT a robust suggestion for how to get permissions in a production app.
  // This is simply minimal sample code in as few lines as possible.
  val allPermissions =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION)
      } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
      }

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

  // FIXME: This is restarting navigation every time the screen is rotated.
  LaunchedEffect(savedInstanceState) {
    // Request all permissions
    permissionsLauncher.launch(allPermissions)

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
      viewModel = core.startNavigation(route = route)

      locationProvider.setSimulatedRoute(route)
    }
  }

  if (viewModel != null) {
    // Demo tiles illustrate a basic integration without any API key required,
    // but you can replace the styleURL with any valid MapLibre style URL.
    // See https://stadiamaps.github.io/ferrostar/vendors.html for some vendors.
    // Most vendors offer free API keys for development use.
    DynamicallyOrientingNavigationView(
        modifier = Modifier.fillMaxSize(),
        // These are demo tiles and not very useful.
        // Check https://stadiamaps.github.io/ferrostar/vendors.html for some vendors of vector
        // tiles.
        // Most vendors offer free API keys for development use.
        styleUrl = "https://demotiles.maplibre.org/style.json",
        viewModel = viewModel!!,
        onTapExit = { viewModel!!.stopNavigation() }) { uiState ->
          // Trivial, if silly example of how to add your own overlay layers.
          // (Also incidentally highlights the lag inherent in MapLibre location tracking
          // as-is.)
          uiState.value.snappedLocation?.let {
            Circle(
                center = LatLng(it.coordinates.lat, it.coordinates.lng),
                radius = 10f,
                color = "Blue",
                zIndex = 2,
            )
          }
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
