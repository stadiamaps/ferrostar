package com.stadiamaps.ferrostar.maplibreui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.mock.MockNavigationViewModel
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.maplibreui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.maplibreui.config.mapControlsFor
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.runtime.AutoHideSystemUIDisposableEffect
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import com.stadiamaps.ferrostar.maplibreui.views.overlays.PortraitNavigationOverlayView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A portrait orientation of the navigation view with instructions, default controls and the
 * navigation map view.
 *
 * @param modifier
 * @param styleUrl
 * @param viewModel
 * @param locationRequestProperties
 */
@Composable
fun PortraitNavigationView(
    modifier: Modifier,
    styleUrl: String,
    camera: MutableState<MapViewCamera> = rememberSaveableMapViewCamera(),
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    onTapExit: (() -> Unit)? = null,
    content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null
) {
  AutoHideSystemUIDisposableEffect()

  Box(modifier) {
    NavigationMapView(
        styleUrl,
        mapControlsFor(isLandscape = false, isArrivalExpanded = onTapExit != null),
        camera,
        navigationCamera,
        viewModel,
        locationRequestProperties,
        onMapReadyCallback = { camera.value = navigationCamera },
        content)

    PortraitNavigationOverlayView(
        modifier =
            Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        config = config,
        camera = camera,
        viewModel = viewModel,
        onTapExit = onTapExit)
  }
}

@Preview(device = Devices.PIXEL_5)
@Composable
private fun PortraitNavigationViewPreview() {
  val viewModel =
      MockNavigationViewModel(
          MutableStateFlow<NavigationUiState>(NavigationUiState.pedestrianExample()).asStateFlow())

  PortraitNavigationView(
      Modifier.fillMaxSize(), "https://demotiles.maplibre.org/style.json", viewModel = viewModel)
}
