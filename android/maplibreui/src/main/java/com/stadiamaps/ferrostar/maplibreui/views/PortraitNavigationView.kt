package com.stadiamaps.ferrostar.maplibreui.views

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.extensions.incrementZoom
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.runtime.paddingForGridView
import com.stadiamaps.ferrostar.composeui.theme.DefaultNavigationUITheme
import com.stadiamaps.ferrostar.composeui.theme.NavigationUITheme
import com.stadiamaps.ferrostar.composeui.views.overlays.PortraitNavigationOverlayView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.boundingBox
import com.stadiamaps.ferrostar.core.mock.MockNavigationViewModel
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.extensions.cameraControlState
import com.stadiamaps.ferrostar.maplibreui.routeline.NavigationPathBuilder
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberMapControlsForProgressViewHeight
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
 * @param navigationPathBuilder The navigation path builder to use for rendering the route line on
 *   the MapView.
 * @param theme The navigation UI theme to use for the view.
 * @param config The configuration for the navigation view.
 * @param views The navigation view component builder to use for the view.
 * @param mapViewInsets The padding inset representing the open area of the map.
 * @param onTapExit The callback to invoke when the exit button is tapped.
 * @param mapContent Any additional composable map symbol content to render.
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
    snapUserLocationToRoute: Boolean = true,
    theme: NavigationUITheme = DefaultNavigationUITheme,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    views: NavigationViewComponentBuilder = NavigationViewComponentBuilder.Default(theme),
    mapViewInsets: MutableState<PaddingValues> = remember { mutableStateOf(PaddingValues(0.dp)) },
    navigationPathBuilder: NavigationPathBuilder = NavigationPathBuilder.Default(),
    onTapExit: (() -> Unit)? = null,
    mapContent: @Composable @MapLibreComposable() ((NavigationUiState) -> Unit)? = null,
) {
  val uiState by viewModel.navigationUiState.collectAsState()

  LaunchedEffect(mapViewInsets.value) {
    Log.d("PortraitNavigationView", "mapViewInsets.value: ${mapViewInsets.value}")
  }

  // Get the correct padding based on edge-to-edge status.
  val gridPadding = paddingForGridView()

  // Get the map control positioning based on the progress view.
  // TODO: I think we should just remove all annotations for nav & make a better tool if needed.
  //  val mapControls = rememberMapControlsForProgressViewHeight(progressViewSize.height)
  val mapControls = rememberMapControlsForProgressViewHeight()

  Box(modifier) {
    NavigationMapView(
        styleUrl,
        camera,
        navigationCamera,
        uiState,
        mapControls,
        locationRequestProperties,
        snapUserLocationToRoute,
        navigationPathBuilder,
        onMapReadyCallback = { camera.value = navigationCamera },
        mapContent)

    if (uiState.isNavigating()) {
      PortraitNavigationOverlayView(
          modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(gridPadding),
          viewModel = viewModel,
          cameraControlState =
              config.cameraControlState(
                  camera = camera,
                  navigationCamera = navigationCamera,
                  mapViewInsets = mapViewInsets.value,
                  boundingBox = uiState.routeGeometry?.boundingBox(),
              ),
          theme = theme,
          config = config,
          onClickZoomIn = { camera.value = camera.value.incrementZoom(1.0) },
          onClickZoomOut = { camera.value = camera.value.incrementZoom(-1.0) },
          views = views,
          mapViewInsets = mapViewInsets,
          onTapExit = onTapExit)

      views.getCustomOverlayView()?.let { customOverlayView ->
        customOverlayView(
            Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(gridPadding))
      }
    }
  }
}

@Preview(device = Devices.PIXEL_5)
@Composable
private fun PortraitNavigationViewPreview() {
  val viewModel =
      MockNavigationViewModel(MutableStateFlow(NavigationUiState.pedestrianExample()).asStateFlow())

  PortraitNavigationView(
      Modifier.fillMaxSize(),
      styleUrl = "https://demotiles.maplibre.org/style.json",
      viewModel = viewModel)
}
