package com.stadiamaps.ferrostar

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.maplibre.compose.symbols.Circle
import com.stadiamaps.autocomplete.center
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.config.withCustomOverlayView
import com.stadiamaps.ferrostar.composeui.runtime.KeepScreenOnDisposableEffect
import com.stadiamaps.ferrostar.core.AndroidSystemLocationProvider
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.googleplayservices.FusedLocationProvider
import com.stadiamaps.ferrostar.maplibreui.views.DynamicallyOrientingNavigationView
import kotlin.math.min
import kotlinx.coroutines.launch

@Composable
fun DemoNavigationScene(
    savedInstanceState: Bundle?,
    locationProvider: LocationProvider = AppModule.locationProvider,
    viewModel: DemoNavigationViewModel = AppModule.viewModel
) {
  // Keeps the screen on at consistent brightness while this Composable is in the view hierarchy.
  KeepScreenOnDisposableEffect()

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

  val navigationUiState by viewModel.navigationUiState.collectAsState(scope.coroutineContext)

  val permissionsLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
          permissions ->
        when {
          permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
            val vm = viewModel
            if ((locationProvider is AndroidSystemLocationProvider ||
                locationProvider is FusedLocationProvider)) {
              // Activate location updates in the view model
              vm.startLocationUpdates(locationProvider)
            }
          }
          permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
            // TODO: Probably alert the user that this is unusable for navigation
          }
          // TODO: Foreground service permissions; we should block access until approved on API 34+
          else -> {
            // TODO
          }
        }
      }

  // FIXME: This is restarting navigation every time the screen is rotated.
  LaunchedEffect(savedInstanceState) {
    // Request all permissions
    permissionsLauncher.launch(allPermissions)
  }

  // For smart casting
  val loc = navigationUiState.location
  if (loc == null) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
      Text("Waiting to acquire your GPS location...", modifier = Modifier.padding(innerPadding))
    }
    return
  }

  // Set up the map!
  val camera = rememberSaveableMapViewCamera(MapViewCamera.TrackingUserLocation())
  DynamicallyOrientingNavigationView(
      modifier = Modifier.fillMaxSize(),
      styleUrl = AppModule.mapStyleUrl,
      camera = camera,
      viewModel = viewModel,
      // Snapping works well for most motor vehicle navigation.
      // Other travel modes though, such as walking, may not want snapping.
      snapUserLocationToRoute = false,
      views =
          NavigationViewComponentBuilder.Default()
              .withCustomOverlayView(
                  customOverlayView = { modifier ->
                    AutocompleteOverlay(
                        modifier = modifier,
                        scope = scope,
                        isNavigating = navigationUiState.isNavigating(),
                        locationProvider = locationProvider,
                        loc = loc)
                  }),
      onTapExit = { viewModel.stopNavigation() }) { uiState ->
        // Trivial, if silly example of how to add your own overlay layers.
        // (Also incidentally highlights the lag inherent in MapLibre location tracking
        // as-is.)
        uiState.location?.let { location ->
          Circle(
              center = LatLng(location.coordinates.lat, location.coordinates.lng),
              radius = 10f,
              color = "Blue",
              zIndex = 3,
          )

          if (location.horizontalAccuracy > 15) {
            Circle(
                center = LatLng(location.coordinates.lat, location.coordinates.lng),
                radius = min(location.horizontalAccuracy.toFloat(), 150f),
                color = "Blue",
                opacity = 0.2f,
                zIndex = 2,
            )
          }
        }
      }
}
