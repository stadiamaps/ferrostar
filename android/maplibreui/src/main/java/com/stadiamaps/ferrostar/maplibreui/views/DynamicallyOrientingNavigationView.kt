package com.stadiamaps.ferrostar.maplibreui.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.runtime.paddingForGridView
import com.stadiamaps.ferrostar.composeui.theme.DefaultNavigationUITheme
import com.stadiamaps.ferrostar.composeui.theme.NavigationUITheme
import com.stadiamaps.ferrostar.composeui.views.overlays.LandscapeNavigationOverlayView
import com.stadiamaps.ferrostar.composeui.views.overlays.PortraitNavigationOverlayView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.boundingBox
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.extensions.cameraControlState
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberMapControlsForProgressViewHeight

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
 * @param snapUserLocationToRoute If true, the user's displayed location will be snapped to the
 *   route line.
 * @param config The configuration for the navigation view.
 * @param onTapExit The callback to invoke when the exit button is tapped.
 * @param userContent Any composable with additional content to render. The most common use of this
 *   parameter is to display custom UI when there is no navigation in progress. See the demo app for
 *   an example that adds a search box.
 * @param mapContent Any additional composable map symbol content to render.
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
    snapUserLocationToRoute: Boolean = true,
    theme: NavigationUITheme = DefaultNavigationUITheme,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    views: NavigationViewComponentBuilder = NavigationViewComponentBuilder.Default(theme),
    mapViewInsets: MutableState<PaddingValues> = remember { mutableStateOf(PaddingValues(0.dp)) },
    onTapExit: (() -> Unit)? = null,
    mapContent: @Composable @MapLibreComposable ((NavigationUiState) -> Unit)? = null,
) {
  val orientation = LocalConfiguration.current.orientation

  // Maintain the actual size of the progress view for dynamic layout purposes.
  val rememberProgressViewSize = remember { mutableStateOf(DpSize.Zero) }
  val progressViewSize by rememberProgressViewSize
  val uiState by viewModel.uiState.collectAsState()

  // Get the correct padding based on edge-to-edge status.
  val gridPadding = paddingForGridView()

  // Get the map control positioning based on the progress view.
  val mapControls = rememberMapControlsForProgressViewHeight(progressViewSize.height)

  Box(modifier) {
    NavigationMapView(
        styleUrl = styleUrl,
        camera = camera,
        navigationCamera = navigationCamera,
        uiState = uiState,
        mapControls = mapControls,
        locationRequestProperties = locationRequestProperties,
        snapUserLocationToRoute = snapUserLocationToRoute,
        content = mapContent)

    if (uiState.isNavigating()) {
      when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
          LandscapeNavigationOverlayView(
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
              views = views,
              mapViewInsets = mapViewInsets,
              onTapExit = onTapExit)
        }

        else -> {
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
              views = views,
              mapViewInsets = mapViewInsets,
              onTapExit = onTapExit)
        }
      }
    }

    views.customOverlayView?.let { customOverlayView ->
      customOverlayView(Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(gridPadding))
    }
  }
}
