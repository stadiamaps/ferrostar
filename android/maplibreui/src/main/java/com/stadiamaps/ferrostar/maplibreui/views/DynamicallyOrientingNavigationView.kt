package com.stadiamaps.ferrostar.maplibreui.views

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera

/**
 * A dynamically orienting navigation view that switches between portrait and landscape orientations
 * based on the device's current orientation.
 *
 * @param modifier The modifier to apply to the view.
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param camera The bi-directional camera state to use for the map.
 * @param viewModel The navigation view model provided by Ferrostar Core.
 * @param locationRequestProperties The location request properties to use for the map's location
 *   engine.
 * @param content Any additional composable map symbol content to render.
 * @param orientation The orientation of the device. Defaults to the current device orientation, but
 *   can be overridden (e.g. with Configuration.ORIENTATION_LANDSCAPE or
 *   Configuration.ORIENTATION_PORTRAIT).
 */
@Composable
fun DynamicallyOrientingNavigationView(
  modifier: Modifier,
  orientation: Int = LocalConfiguration.current.orientation,
  styleUrl: String,
  camera: MutableState<MapViewCamera> = rememberSaveableMapViewCamera(),
  navigationCamera: MapViewCamera = navigationMapViewCamera(),
  viewModel: NavigationViewModel,
  locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
  onTapExit: (() -> Unit)? = null,
  content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null,
) {
  when (orientation) {
    Configuration.ORIENTATION_LANDSCAPE -> {
      LandscapeNavigationView(
          modifier, styleUrl, camera, navigationCamera, viewModel, locationRequestProperties, onTapExit, content)
    }
    else -> {
      PortraitNavigationView(
          modifier, styleUrl, camera, navigationCamera, viewModel, locationRequestProperties, onTapExit, content)
    }
  }
}
