package com.stadiamaps.ferrostar.maplibreui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
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
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import com.stadiamaps.ferrostar.maplibreui.views.overlays.LandscapeNavigationOverlayView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A portrait orientation of the navigation view with instructions, default controls and the
 * navigation map view.
 *
 * @param modifier The modifier to apply to the view.
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param camera The bi-directional camera state to use for the map.
 * @param navigationCamera The default camera state to use for navigation. This is applied on launch
 *   and when centering.
 * @param viewModel The navigation view model provided by Ferrostar Core.
 * @param locationRequestProperties The location request properties to use for the map's location
 *   engine.
 * @param snapUserLocationToRoute If true, the user's displayed location will be snapped to the
 *   route line.
 * @param config The configuration for the navigation view.
 * @param overlayModifier The modifier to apply to the overlay view.
 * @param onTapExit The callback to invoke when the exit button is tapped.
 * @param content Any additional composable map symbol content to render.
 */
@Composable
fun LandscapeNavigationView(
    modifier: Modifier,
    styleUrl: String,
    camera: MutableState<MapViewCamera> = rememberSaveableMapViewCamera(),
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    snapUserLocationToRoute: Boolean = true,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    overlayModifier: Modifier = Modifier.fillMaxSize().padding(16.dp),
    onTapExit: (() -> Unit)? = null,
    content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null
) {
  Box(modifier) {
    NavigationMapView(
        styleUrl,
        mapControlsFor(isLandscape = true, isArrivalExpanded = onTapExit != null),
        camera,
        navigationCamera,
        viewModel,
        locationRequestProperties,
        snapUserLocationToRoute,
        onMapReadyCallback = { camera.value = navigationCamera },
        content)

    LandscapeNavigationOverlayView(
        modifier = overlayModifier,
        config = config,
        camera = camera,
        viewModel = viewModel,
        onTapExit = onTapExit)
  }
}

@Preview(
    device =
        "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
private fun LandscapeNavigationViewPreview() {
  val viewModel =
      MockNavigationViewModel(
          MutableStateFlow<NavigationUiState>(NavigationUiState.pedestrianExample()).asStateFlow())

  LandscapeNavigationView(
      Modifier.fillMaxSize(),
      styleUrl = "https://demotiles.maplibre.org/style.json",
      viewModel = viewModel)
}
