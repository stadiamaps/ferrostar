package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.maps.Style
import com.maplibre.compose.MapView
import com.maplibre.compose.StaticLocationEngine
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.settings.MapControls
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.toAndroidLocation
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.routeline.NavigationPathBuilder
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
 * @param uiState The navigation UI state.
 * @param locationRequestProperties The location request properties to use for the map's location
 *   engine.
 * @param snapUserLocationToRoute If true, the user's displayed location will be snapped to the
 *   route line.
 * @param showCompleteRoute If true, the complete route will be displayed. If false, only the
 *   remaining geometry will be displayed on the polyline.
 * @param onMapReadyCallback A callback that is invoked when the map is ready to be interacted with.
 *   If unspecified, the camera will change to `navigationCamera` if navigation is in progress.
 * @param content Any additional composable map symbol content to render.
 */
@Composable
fun NavigationMapView(
    styleUrl: String,
    camera: MutableState<MapViewCamera>,
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    uiState: NavigationUiState,
    mapControls: State<MapControls>,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    snapUserLocationToRoute: Boolean = true,
    navigationPathBuilder: NavigationPathBuilder = NavigationPathBuilder.Default(),
    onMapReadyCallback: ((Style) -> Unit)? = null,
    content: @Composable @MapLibreComposable ((NavigationUiState) -> Unit)? = null
) {
  var isNavigating by remember { mutableStateOf(uiState.isNavigating()) }
  if (uiState.isNavigating() != isNavigating) {
    isNavigating = uiState.isNavigating()

    if (isNavigating) {
      camera.value = navigationCamera
    }
  }

  val locationEngine = remember { StaticLocationEngine() }
  locationEngine.lastLocation =
      if (snapUserLocationToRoute && isNavigating) {
        uiState.snappedLocation?.toAndroidLocation()
      } else {
        uiState.location?.toAndroidLocation()
      }

  MapView(
      modifier = Modifier.fillMaxSize(),
      styleUrl,
      camera,
      mapControls,
      locationRequestProperties = locationRequestProperties,
      locationEngine = locationEngine,
      onMapReadyCallback =
          onMapReadyCallback ?: { if (isNavigating) camera.value = navigationCamera },
  ) {
    navigationPathBuilder.navigationPath(uiState)

    if (content != null) {
      content(uiState)
    }
  }
}
