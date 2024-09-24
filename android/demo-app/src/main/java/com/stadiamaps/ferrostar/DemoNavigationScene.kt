package com.stadiamaps.ferrostar

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.mapboxsdk.geometry.LatLng
import com.maplibre.compose.symbols.Circle
import com.stadiamaps.autocomplete.AutocompleteSearch
import com.stadiamaps.autocomplete.center
import com.stadiamaps.ferrostar.composeui.runtime.KeepScreenOnDisposableEffect
import com.stadiamaps.ferrostar.core.AndroidSystemLocationProvider
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.maplibreui.views.DynamicallyOrientingNavigationView
import com.stadiamaps.ferrostar.support.initialSimulatedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind
import java.util.concurrent.Executors

@Composable
fun DemoNavigationScene(
  savedInstanceState: Bundle?,
  locationProvider: LocationProvider = AppModule.locationProvider,
  core: FerrostarCore = AppModule.ferrostarCore
) {
  val executor = remember {
    Executors.newSingleThreadScheduledExecutor()
  }

  // Keeps the screen on at consistent brightness while this Composable is in the view hierarchy.
  KeepScreenOnDisposableEffect()

  var viewModel by remember { mutableStateOf<NavigationViewModel?>(null) }
    val scope = rememberCoroutineScope()

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
            // TODO: Fused
            if (locationProvider is AndroidSystemLocationProvider) {
              locationProvider.addListener(this@, executor)
            }
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
  }

    if (viewModel == null) {
        // TODO: How to work without an API key
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            AutocompleteSearch(
                modifier = Modifier.padding(innerPadding),
                apiKey = "68b6fcab-7a23-4cfd-a3d9-234389ef1b68"
            ) { feature ->
                // Fetch a route in the background
                scope.launch(Dispatchers.IO) {
                    // TODO: Fail gracefully
                    val center = feature.center()!!
                    val routes =
                        core.getRoutes(
                            // FIXME
                            initialSimulatedLocation,
                            listOf(
                                Waypoint(
                                    coordinate = GeographicCoordinate(center.latitude, center.longitude),
                                    kind = WaypointKind.BREAK),
                            ))

                    val route = routes.first()
                    viewModel = core.startNavigation(route = route)

                  if (locationProvider is SimulatedLocationProvider) {
                    locationProvider.setSimulatedRoute(route)
                  }
                }
            }
        }
    } else {
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
        // TODO: Make it nullable
        viewModel = viewModel!!,
        // This is the default value, which works well for motor vehicle navigation.
        // Other travel modes though, such as walking, may not want snapping.
        snapUserLocationToRoute = true,
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
  }
//    else {
//    // Loading indicator
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally) {
//          Text(text = "Calculating route...")
//          CircularProgressIndicator(modifier = Modifier.width(64.dp))
//        }
//  }
}
