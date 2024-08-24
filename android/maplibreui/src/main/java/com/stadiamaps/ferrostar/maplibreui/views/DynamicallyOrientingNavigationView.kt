package com.stadiamaps.ferrostar.maplibreui.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.maplibreui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.maplibreui.config.mapControlsFor
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationActivity
import com.stadiamaps.ferrostar.maplibreui.runtime.SystemUIDisposableEffect
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import com.stadiamaps.ferrostar.maplibreui.views.overlays.LandscapeNavigationOverlayView
import com.stadiamaps.ferrostar.maplibreui.views.overlays.PortraitNavigationOverlayView

/**
 * A dynamically orienting navigation view that switches between portrait and landscape orientations
 * based on the device's current orientation.
 *
 * @param modifier The modifier to apply to the view.
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param camera The bi-directional camera state to use for the map.
 * @param navigationCamera The default camera state to use for navigation. This is applied on launch
 *   and when centering.
 * @param viewModel The navigation view model provided by Ferrostar Core.
 * @param locationRequestProperties The location request properties to use for the map's location
 *   engine.
 * @param config The configuration for the navigation view.
 * @param content Any additional composable map symbol content to render.
 */
@Composable
fun DynamicallyOrientingNavigationView(
    modifier: Modifier,
    styleUrl: String,
    camera: MutableState<MapViewCamera> = rememberSaveableMapViewCamera(),
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    onTapExit: (() -> Unit)? = null,
    content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null,
) {
  SystemUIDisposableEffect()

  val orientation = LocalConfiguration.current.orientation
  val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

  Box(modifier) {
    NavigationMapView(
        styleUrl,
        mapControlsFor(isLandscape = isLandscape, isArrivalExpanded = onTapExit != null),
        camera,
        navigationCamera,
        viewModel,
        locationRequestProperties,
        onMapReadyCallback = { camera.value = navigationCamera },
        content)

    when (orientation) {
      Configuration.ORIENTATION_LANDSCAPE -> {
        LandscapeNavigationOverlayView(
            modifier =
                Modifier.fillMaxSize()
                  // TODO: DO NOT MERGE - Finalize w/ device
                    .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            camera = camera,
            viewModel = viewModel,
            config = config,
            onTapExit = onTapExit)
      }
      else -> {
        PortraitNavigationOverlayView(
            modifier =
                Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            camera = camera,
            viewModel = viewModel,
            config = config,
            onTapExit = onTapExit)
      }
    }
  }
}
