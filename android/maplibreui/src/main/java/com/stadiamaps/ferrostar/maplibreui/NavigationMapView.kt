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
import com.maplibre.compose.MapView
import com.maplibre.compose.StaticLocationEngine
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.settings.MapControls
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.toAndroidLocation
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.routeline.RouteOverlayBuilder
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import org.maplibre.android.maps.Style

/**
 * The base MapLibre MapView configured for navigation with a polyline representing the route.
 *
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param camera The bi-directional camera state to use for the map. Note: this is a bit
 *   non-standard as far as normal compose patterns go, but we independently came up with this
 *   approach and later verified that Google Maps does the same thing in their compose SDK.
 * @param navigationCamera The default camera state to use for navigation. This is a *template*
 *   value, which will be applied on initial display and when re-centering. The default value is
 *   sufficient for most applications. If you set a custom value (e.g.) to change the pitch), you
 *   must ensure that it is some variation on [MapViewCamera.TrackingUserLocationWithBearing].
 * @param uiState The navigation UI state.
 * @param locationRequestProperties The location request properties to use for the map's location
 *   engine.
 * @param routeOverlayBuilder The route overlay builder to use for rendering the route line on the
 *   MapView.
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
    routeOverlayBuilder: RouteOverlayBuilder = RouteOverlayBuilder.Default(),
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
  locationEngine.lastLocation = uiState.location?.toAndroidLocation()

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
    routeOverlayBuilder.navigationPath(uiState)

    if (content != null) {
      content(uiState)
    }
  }
}
