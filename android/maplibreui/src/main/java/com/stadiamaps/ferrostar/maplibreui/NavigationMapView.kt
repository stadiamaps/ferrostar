package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import com.maplibre.compose.MapView
import com.maplibre.compose.StaticLocationEngine
import com.maplibre.compose.camera.CameraPitch
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationPriority
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.stadiamaps.ferrostar.composeui.views.InstructionsView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.toAndroidLocation
import uniffi.ferrostar.VisualInstruction

@Composable
fun NavigationMapView(
    styleUrl: String,
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.Builder()
            .priority(LocationPriority.PRIORITY_HIGH_ACCURACY)
            .interval(1000L)
            .fastestInterval(0L)
            .displacement(0F)
            .maxWaitTime(1000L)
            .build(),
    bannerContentBuilder: @Composable (VisualInstruction, Double?) -> Unit =
        { instruction, distanceToNextManeuver ->
          InstructionsView(instruction, distanceToNextManeuver)
        },
    content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null
) {
  val uiState = viewModel.uiState.collectAsState()
  val locationEngine = remember { StaticLocationEngine() }
  locationEngine.lastLocation = uiState.value.snappedLocation.toAndroidLocation()
  val camera = rememberSaveable { mutableStateOf(MapViewCamera()) }
  // FIXME: Pitch is not being propagated
  camera.value = MapViewCamera.TrackingUserLocationWithBearing(18.0, CameraPitch.Fixed(45.0))

  Box {
    MapView(
        modifier = Modifier.fillMaxSize(),
        styleUrl = styleUrl,
        camera = camera,
        locationRequestProperties = locationRequestProperties,
        locationEngine = locationEngine,
    ) {
      BorderedPolyline(
          points = uiState.value.routeGeometry.map { LatLng(it.lat, it.lng) }, zIndex = 0)

      if (content != null) {
        content(uiState)
      }
    }

    uiState.value.visualInstruction?.let {
      bannerContentBuilder(it, uiState.value.distanceToNextManeuver)
    }
  }
}
