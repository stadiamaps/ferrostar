package expo.modules.ferrostar

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.runtime.KeepScreenOnDisposableEffect
import com.stadiamaps.ferrostar.core.AndroidSystemLocationProvider
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.googleplayservices.FusedLocationProvider
import com.stadiamaps.ferrostar.maplibreui.BorderedPolyline
import com.stadiamaps.ferrostar.maplibreui.views.DynamicallyOrientingNavigationView
import expo.modules.ferrostar.records.NavigationOptions
import expo.modules.ferrostar.ui.FerrostarTheme
import uniffi.ferrostar.Route

@Composable
fun NavigationScene(viewModel: ExpoNavigationViewModel, locationProvider: LocationProvider, options: NavigationOptions) {
    KeepScreenOnDisposableEffect()

    val scope = rememberCoroutineScope()

    val allPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    val navigationUiState by viewModel.navigationUiState.collectAsState(scope.coroutineContext)
    val previewRoute by viewModel.previewRoute.collectAsState(scope.coroutineContext)

    val permissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    if (locationProvider is AndroidSystemLocationProvider
                        || locationProvider is FusedLocationProvider) {
                        // Activate location updates in the view model
                        viewModel.startLocationUpdates(locationProvider)
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

    LaunchedEffect(locationProvider) {
        Log.d("NavigationScene", "Launched effect: $allPermissions")
        permissionsLauncher.launch(allPermissions)
    }

    val camera = rememberSaveableMapViewCamera(MapViewCamera.TrackingUserLocation())
    FerrostarTheme {
      DynamicallyOrientingNavigationView(
          modifier = Modifier.fillMaxSize(),
          styleUrl = options.styleUrl,
          camera = camera,
          viewModel = viewModel,
          snapUserLocationToRoute = options.snapUserLocationToRoute,
          views = NavigationViewComponentBuilder.Default(),
          onTapExit = { viewModel.stopNavigation() }
      ) {
          if (previewRoute != null && !navigationUiState.isNavigating()) {
              BorderedPolyline(points = previewRoute!!.geometry.map { LatLng(it.lat, it.lng) }, zIndex = 0)
          }
      }
    }
}
