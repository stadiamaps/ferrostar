package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import com.maplibre.compose.MapView
import com.maplibre.compose.StaticLocationEngine
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.toAndroidLocation
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault

/**
 * The base MapLibre MapView configured for navigation with a polyline representing the route.
 *
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param camera The bi-directional camera state to use for the map.
 * @param viewModel The navigation view model provided by Ferrostar Core.
 * @param locationRequestProperties The location request properties to use for the map's location
 *   engine.
 * @param content Any additional composable map symbol content to render.
 */
@Composable
fun NavigationMapView(
    styleUrl: String,
    camera: MutableState<MapViewCamera>,
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null
) {
  val uiState = viewModel.uiState.collectAsState()

  val locationEngine = remember { StaticLocationEngine() }
  locationEngine.lastLocation = uiState.value.snappedLocation.toAndroidLocation()

  MapView(
      modifier = Modifier.fillMaxSize(),
      styleUrl = styleUrl,
      camera = camera,
      locationRequestProperties = locationRequestProperties,
      locationEngine = locationEngine,
      onMapReadyCallback = {
        // Set the camera to navigation on resume.
        camera.value = MapViewCamera.NavigationDefault()

        // Any addition map view related setup should happen here to ensure the map is ready (style
        // is loaded).
      },
  ) {
    BorderedPolyline(
        points = uiState.value.routeGeometry.map { LatLng(it.lat, it.lng) }, zIndex = 0)

    if (content != null) {
      content(uiState)
    }
  }
}
