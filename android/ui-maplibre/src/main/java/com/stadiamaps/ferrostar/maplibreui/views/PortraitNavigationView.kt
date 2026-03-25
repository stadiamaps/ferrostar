package com.stadiamaps.ferrostar.maplibreui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.runtime.paddingForGridView
import com.stadiamaps.ferrostar.composeui.theme.DefaultNavigationUITheme
import com.stadiamaps.ferrostar.composeui.theme.NavigationUITheme
import com.stadiamaps.ferrostar.composeui.views.overlays.PortraitNavigationOverlayView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.boundingBox
import com.stadiamaps.ferrostar.maplibreui.NavigationMapClickHandler
import com.stadiamaps.ferrostar.maplibreui.NavigationMapClickResult
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.maplibreui.NavigationMapPuckStyle
import com.stadiamaps.ferrostar.maplibreui.extensions.cameraControlState
import com.stadiamaps.ferrostar.maplibreui.routeline.RouteOverlayBuilder
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationCameraOptions
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationMapState
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationCameraOptions
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberMapOptionsForProgressViewHeight
import com.stadiamaps.ferrostar.maplibreui.runtime.rememberNavigationMapState
import org.maplibre.compose.util.MaplibreComposable

/**
 * A portrait orientation of the navigation view with instructions, default controls and the
 * navigation map view.
 *
 * @param modifier The modifier to apply to the view.
 * @param styleUrl The MapLibre style URL to use for the map.
 * @param navigationMapState The Ferrostar-owned map state used to coordinate follow, overview,
 *   free-camera behavior, and zoom actions.
 * @param navigationCameraOptions The camera templates applied when following the user in browsing
 *   and navigation modes.
 * @param viewModel The navigation view model (see
 *   [com.stadiamaps.ferrostar.core.DefaultNavigationViewModel] for a common implementation]).
 * @param locationPuckStyle The style to use for the official MapLibre location puck.
 * @param theme The navigation UI theme to use for the view.
 * @param config The configuration for the navigation view.
 * @param views The navigation view component builder to use for the view.
 * @param mapViewInsets The padding inset representing the open area of the map.
 * @param routeOverlayBuilder The route overlay builder to use for rendering the route line.
 * @param onTapExit The callback to invoke when the exit button is tapped.
 * @param onMapClick Callback invoked for taps on the map with geographic coordinates and screen
 *   position.
 * @param onMapLongClick Callback invoked for long presses on the map with geographic coordinates
 *   and screen position.
 * @param mapContent Any additional composable map symbol content to render.
 */
@Composable
fun PortraitNavigationView(
    modifier: Modifier,
    styleUrl: String,
    navigationMapState: NavigationMapState = rememberNavigationMapState(),
    navigationCameraOptions: NavigationCameraOptions = navigationCameraOptions(),
    viewModel: NavigationViewModel,
    locationPuckStyle: NavigationMapPuckStyle = NavigationMapPuckStyle(),
    theme: NavigationUITheme = DefaultNavigationUITheme,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    views: NavigationViewComponentBuilder = NavigationViewComponentBuilder.Default(theme),
    mapViewInsets: MutableState<PaddingValues> = remember { mutableStateOf(PaddingValues(0.dp)) },
    routeOverlayBuilder: RouteOverlayBuilder = RouteOverlayBuilder.Default(),
    onTapExit: (() -> Unit)? = null,
    onMapClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    onMapLongClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    mapContent: @Composable @MaplibreComposable ((NavigationUiState) -> Unit)? = null,
) {
  val uiState by viewModel.navigationUiState.collectAsState()
  val gridPadding = paddingForGridView()
  val mapOptions = rememberMapOptionsForProgressViewHeight()

  Box(modifier) {
    NavigationMapView(
        styleUrl = styleUrl,
        navigationMapState = navigationMapState,
        uiState = uiState,
        mapOptions = mapOptions,
        navigationCameraOptions = navigationCameraOptions,
        routeOverlayBuilder = routeOverlayBuilder,
        locationPuckStyle = locationPuckStyle,
        onMapClick = onMapClick,
        onMapLongClick = onMapLongClick,
        content = mapContent,
    )

    if (uiState.isNavigating()) {
      PortraitNavigationOverlayView(
          modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(gridPadding),
          viewModel = viewModel,
          cameraControlState =
              config.cameraControlState(
                  navigationMapState = navigationMapState,
                  isNavigating = true,
                  mapViewInsets = mapViewInsets.value,
                  boundingBox = uiState.routeGeometry?.boundingBox(),
              ),
          theme = theme,
          config = config,
          onClickZoomIn = { navigationMapState.zoomIn() },
          onClickZoomOut = { navigationMapState.zoomOut() },
          views = views,
          mapViewInsets = mapViewInsets,
          onTapExit = onTapExit,
      )

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
  PortraitNavigationView(
      modifier = Modifier.fillMaxSize(),
      styleUrl = "https://demotiles.maplibre.org/style.json",
      viewModel = previewViewModel,
  )
}
