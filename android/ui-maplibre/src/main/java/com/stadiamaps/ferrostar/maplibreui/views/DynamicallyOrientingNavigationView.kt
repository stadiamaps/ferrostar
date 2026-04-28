package com.stadiamaps.ferrostar.maplibreui.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.stadiamaps.ferrostar.maplibreui.runtime.withNavigationBottomInset
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.compose.style.BaseStyle

/**
 * A dynamically orienting navigation view that switches between portrait and landscape overlays
 * based on the current device orientation.
 *
 * @param ornamentPadding Optional padding applied to built-in map ornaments such as the logo and
 * attribution. Defaults to `WindowInsets.systemBars` when not provided.
 * @param overlayPadding Optional padding applied to Ferrostar-owned overlay chrome such as
 * instructions, controls, and custom overlays. Defaults to `WindowInsets.systemBars` when not
 * provided.
 */
@Composable
fun DynamicallyOrientingNavigationView(
    modifier: Modifier,
    baseStyle: BaseStyle,
    navigationMapState: NavigationMapState = rememberNavigationMapState(),
    navigationCameraOptions: NavigationCameraOptions = navigationCameraOptions(),
    viewModel: NavigationViewModel,
    locationPuckStyle: NavigationMapPuckStyle = NavigationMapPuckStyle(),
    theme: NavigationUITheme = DefaultNavigationUITheme,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    views: NavigationViewComponentBuilder = NavigationViewComponentBuilder.Default(theme),
    mapViewInsets: MutableState<PaddingValues> = remember { mutableStateOf(PaddingValues(0.dp)) },
    ornamentPadding: PaddingValues? = null,
    overlayPadding: PaddingValues? = null,
    routeOverlayBuilder: RouteOverlayBuilder? = RouteOverlayBuilder.Default(),
    showDefaultPuck: Boolean = true,
    onTapExit: (() -> Unit)? = null,
    onMapClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    onMapLongClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    mapContent: @Composable @MaplibreComposable ((NavigationUiState) -> Unit)? = null,
) {
  val configuration = LocalConfiguration.current
  val orientation = configuration.orientation
  val layoutDirection = LocalLayoutDirection.current

  var progressViewHeight by remember { mutableStateOf(0.dp) }
  val uiState by viewModel.navigationUiState.collectAsState()

  val gridPadding = paddingForGridView()
  val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
  val resolvedOrnamentPadding = ornamentPadding ?: systemBarsPadding
  val resolvedOverlayPadding = overlayPadding ?: systemBarsPadding
  val mapOptions = rememberMapOptionsForProgressViewHeight(
      progressViewHeight = if (uiState.isNavigating()) progressViewHeight else 0.dp,
      contentPadding = resolvedOrnamentPadding,
  )
  val effectiveNavigationCameraOptions =
      if (uiState.isNavigating()) {
        navigationCameraOptions.withNavigationBottomInset(
            bottomInset = mapViewInsets.value.calculateBottomPadding(),
            screenHeight = configuration.screenHeightDp.dp,
            layoutDirection = layoutDirection,
        )
      } else {
        navigationCameraOptions
      }

  Box(modifier) {
    NavigationMapView(
        baseStyle = baseStyle,
        navigationMapState = navigationMapState,
        uiState = uiState,
        mapOptions = mapOptions,
        navigationCameraOptions = effectiveNavigationCameraOptions,
        routeOverlayBuilder = routeOverlayBuilder,
        locationPuckStyle = locationPuckStyle,
        showDefaultPuck = showDefaultPuck,
        onMapClick = onMapClick,
        onMapLongClick = onMapLongClick,
        content = mapContent,
    )

    if (uiState.isNavigating()) {
      when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
          LandscapeNavigationOverlayView(
              modifier = Modifier
                  .padding(resolvedOverlayPadding)
                  .padding(gridPadding),
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
              contentPadding = resolvedOverlayPadding,
              onTapExit = onTapExit,
          )
        }

        else -> {
          PortraitNavigationOverlayView(
              modifier = Modifier
                  .padding(resolvedOverlayPadding)
                  .padding(gridPadding),
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
              contentPadding = resolvedOverlayPadding,
              onProgressViewHeightChange = { progressViewHeight = it },
              onTapExit = onTapExit,
          )
        }
      }
    }

    views.getCustomOverlayView()?.let { customOverlayView ->
      customOverlayView(Modifier
          .padding(resolvedOverlayPadding)
          .padding(gridPadding))
    }
  }
}
