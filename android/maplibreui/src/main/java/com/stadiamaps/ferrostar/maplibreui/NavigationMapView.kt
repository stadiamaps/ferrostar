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
import com.mapbox.mapboxsdk.maps.Style
import com.maplibre.compose.MapView
import com.maplibre.compose.StaticLocationEngine
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.settings.MapControls
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.toAndroidLocation
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationCentered
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault

/**
 * The base MapLibre MapView configured for navigation with a polyline representing the route.
 *
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param camera The bi-directional camera state to use for the map.
 * @param viewModel The navigation view model provided by Ferrostar Core.
 * @param locationRequestProperties The location request properties to use for the map's location
 *   engine.
 * @param onMapReadyCallback A callback that is invoked when the map is ready to be interacted with.
 *   You must set your desired MapViewCamera tracking mode here!
 * @param content Any additional composable map symbol content to render.
 */
@Composable
fun NavigationMapView(
    styleUrl: String,
    mapControls: MapControls,
    camera: MutableState<MapViewCamera>,
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    onMapReadyCallback: (Style) -> Unit = { camera.value = MapViewCamera.NavigationCentered() },
    content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null
) {
  val uiState = viewModel.uiState.collectAsState()

  val locationEngine = remember { StaticLocationEngine() }
  locationEngine.lastLocation = uiState.value.snappedLocation.toAndroidLocation()

  MapView(
      modifier = Modifier.fillMaxSize(),
      styleUrl = styleUrl,
      mapControls = mapControls,
      camera = camera,
      locationRequestProperties = locationRequestProperties,
      locationEngine = locationEngine,
      onMapReadyCallback = onMapReadyCallback,
  ) {
    BorderedPolyline(
        points = uiState.value.routeGeometry.map { LatLng(it.lat, it.lng) }, zIndex = 0)

    if (content != null) {
      content(uiState)
    }
  }
}
