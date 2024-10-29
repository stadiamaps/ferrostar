package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
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
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera

/**
 * The base MapLibre MapView configured for navigation with a polyline representing the route.
 *
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param camera The bi-directional camera state to use for the map. Note: this is a bit
 *   non-standard as far as normal compose patterns go, but we independently came up with this
 *   approach and later verified that Google Maps does the same thing in their compose SDK.
 * @param navigationCamera The default camera settings to use when navigation starts. This will be
 *   re-applied to the camera any time that navigation is started.
 * @param viewModel The navigation view model provided by Ferrostar Core.
 * @param locationRequestProperties The location request properties to use for the map's location
 *   engine.
 * @param snapUserLocationToRoute If true, the user's displayed location will be snapped to the
 *   route line.
 * @param onMapReadyCallback A callback that is invoked when the map is ready to be interacted with.
 *   You must set your desired MapViewCamera tracking mode here!
 * @param content Any additional composable map symbol content to render.
 */
@Composable
fun NavigationMapView(
    styleUrl: String,
    camera: MutableState<MapViewCamera>,
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    viewModel: NavigationViewModel,
    mapControls: State<MapControls>,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    snapUserLocationToRoute: Boolean = true,
    onMapReadyCallback: (Style) -> Unit = {
      if (viewModel.isNavigating()) camera.value = navigationCamera
    },
    content: @Composable @MapLibreComposable ((State<NavigationUiState>) -> Unit)? = null
) {
  val uiState = viewModel.uiState.collectAsState()

  // TODO: This works for now, but in the end, the view model may need to "own" the camera.
  // We can move this code if we do such a refactor.
  var isNavigating = remember { viewModel.isNavigating() }
  if (viewModel.isNavigating() != isNavigating) {
    isNavigating = viewModel.isNavigating()

    if (isNavigating) {
      camera.value = navigationCamera
    }
  }

  val locationEngine = remember { StaticLocationEngine() }
  locationEngine.lastLocation =
      uiState.value.let { state ->
        if (snapUserLocationToRoute) {
          state.snappedLocation?.toAndroidLocation()
        } else {
          state.location?.toAndroidLocation()
        }
      }

  MapView(
      modifier = Modifier.fillMaxSize(),
      styleUrl,
      camera,
      mapControls,
      locationRequestProperties = locationRequestProperties,
      locationEngine = locationEngine,
      onMapReadyCallback = onMapReadyCallback,
  ) {
    val geometry = uiState.value.routeGeometry
    if (geometry != null)
        BorderedPolyline(points = geometry.map { LatLng(it.lat, it.lng) }, zIndex = 0)

    if (content != null) {
      content(uiState)
    }
  }
}
