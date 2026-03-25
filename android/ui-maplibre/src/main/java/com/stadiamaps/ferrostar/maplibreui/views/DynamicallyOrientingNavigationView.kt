package com.stadiamaps.ferrostar.maplibreui.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
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
 * A dynamically orienting navigation view that switches between portrait and landscape overlays
 * based on the current device orientation.
 */
@Composable
fun DynamicallyOrientingNavigationView(
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
  val orientation = LocalConfiguration.current.orientation

  val rememberProgressViewSize = remember { mutableStateOf(DpSize.Zero) }
  val progressViewSize by rememberProgressViewSize
  val uiState by viewModel.navigationUiState.collectAsState()

  val gridPadding = paddingForGridView()
  val mapOptions = rememberMapOptionsForProgressViewHeight(progressViewSize.height)

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
      when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
          LandscapeNavigationOverlayView(
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
        }

        else -> {
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
        }
      }
    }

    views.getCustomOverlayView()?.let { customOverlayView ->
      customOverlayView(Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(gridPadding))
    }
  }
}
