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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.runtime.paddingForGridView
import com.stadiamaps.ferrostar.composeui.theme.DefaultNavigationUITheme
import com.stadiamaps.ferrostar.composeui.theme.NavigationUITheme
import com.stadiamaps.ferrostar.composeui.views.overlays.LandscapeNavigationOverlayView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.boundingBox
import com.stadiamaps.ferrostar.core.mock.MockNavigationViewModel
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.compose.style.BaseStyle

@Composable
fun LandscapeNavigationView(
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
    routeOverlayBuilder: RouteOverlayBuilder? = RouteOverlayBuilder.Default(),
    showDefaultPuck: Boolean = true,
    onTapExit: (() -> Unit)? = null,
    onMapClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    onMapLongClick: NavigationMapClickHandler = { _, _ -> NavigationMapClickResult.Pass },
    mapContent: @Composable @MaplibreComposable ((NavigationUiState) -> Unit)? = null,
) {
  val uiState by viewModel.navigationUiState.collectAsState()
  val configuration = androidx.compose.ui.platform.LocalConfiguration.current
  val layoutDirection = LocalLayoutDirection.current
  val gridPadding = paddingForGridView()
  val mapOptions = rememberMapOptionsForProgressViewHeight()
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
        showDefaultPuck = showDefaultPuck,
        locationPuckStyle = locationPuckStyle,
        onMapClick = onMapClick,
        onMapLongClick = onMapLongClick,
        content = mapContent,
    )

    LandscapeNavigationOverlayView(
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(gridPadding),
        viewModel = viewModel,
        cameraControlState =
            config.cameraControlState(
                navigationMapState = navigationMapState,
                isNavigating = uiState.isNavigating(),
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
      customOverlayView(Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(gridPadding))
    }
  }
}

val previewViewModel =
    MockNavigationViewModel(
        MutableStateFlow<NavigationUiState>(NavigationUiState.pedestrianExample()).asStateFlow())

@Preview(
    device =
        "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
private fun LandscapeNavigationViewPreview() {
  LandscapeNavigationView(
      modifier = Modifier.fillMaxSize(),
      baseStyle = BaseStyle.Uri("https://demotiles.maplibre.org/style.json"),
      viewModel = previewViewModel,
  )
}
