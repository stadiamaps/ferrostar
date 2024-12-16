package com.stadiamaps.ferrostar.composeui.views.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.models.CameraControlState
import com.stadiamaps.ferrostar.composeui.models.NavigationViewMetrics
import com.stadiamaps.ferrostar.composeui.theme.DefaultNavigationUITheme
import com.stadiamaps.ferrostar.composeui.theme.NavigationUITheme
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.NavigatingInnerGridView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.mock.MockNavigationViewModel
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun LandscapeNavigationOverlayView(
    modifier: Modifier,
    viewModel: NavigationViewModel,
    cameraControlState: CameraControlState,
    theme: NavigationUITheme = DefaultNavigationUITheme,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    onClickZoomIn: (() -> Unit)? = null,
    onClickZoomOut: (() -> Unit)? = null,
    views: NavigationViewComponentBuilder = NavigationViewComponentBuilder.Default(theme),
    mapViewInsets: MutableState<PaddingValues>,
    onTapExit: (() -> Unit)? = null,
) {
  val density = LocalDensity.current
  val windowInsets = WindowInsets.statusBars.asPaddingValues()
  val halfOfScreen: Dp = with(density) { LocalConfiguration.current.screenWidthDp.dp / 2 }

  val uiState by viewModel.navigationUiState.collectAsState()

  var instructionsViewSize by remember { mutableStateOf(DpSize.Zero) }
  var progressViewSize by remember { mutableStateOf(DpSize.Zero) }

  mapViewInsets.value =
      NavigationViewMetrics(buttonSize = theme.buttonSize)
          .mapViewInsets(
              start = halfOfScreen + 16.dp,
              top = 16.dp + windowInsets.calculateTopPadding(),
              bottom = 16.dp + windowInsets.calculateBottomPadding())

  Row(modifier) {
    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f)) {
      views.instructionsView(
          Modifier.onSizeChanged {
            instructionsViewSize = density.run { DpSize(it.width.toDp(), it.height.toDp()) }
          },
          uiState)

      Spacer(modifier = Modifier.weight(1f))

      views.progressView(
          Modifier.onSizeChanged {
            progressViewSize = density.run { DpSize(it.width.toDp(), it.height.toDp()) }
          },
          uiState,
          onTapExit)
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.fillMaxHeight()) {
      NavigatingInnerGridView(
          modifier = Modifier.fillMaxSize(),
          speedLimit = uiState.currentAnnotation?.speedLimit,
          speedLimitStyle = config.speedLimitStyle,
          showMute = config.showMute,
          isMuted = uiState.isMuted,
          onClickMute = { viewModel.toggleMute() },
          buttonSize = theme.buttonSize,
          cameraControlState = cameraControlState,
          showZoom = config.showZoom,
          onClickZoomIn = { onClickZoomIn?.invoke() },
          onClickZoomOut = { onClickZoomOut?.invoke() },
          bottomCenter = {
            views.streetNameView(Modifier, uiState.currentStepRoadName, cameraControlState)
          })
    }
  }
}

@Composable
@Preview(
    device =
        "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
fun LandscapeNavigationOverlayViewPreview() {
  val viewModel =
      MockNavigationViewModel(
          MutableStateFlow<NavigationUiState>(NavigationUiState.pedestrianExample()).asStateFlow())

  LandscapeNavigationOverlayView(
      modifier = Modifier.fillMaxSize(),
      viewModel = viewModel,
      cameraControlState = CameraControlState.Hidden,
      mapViewInsets = remember { mutableStateOf(PaddingValues()) },
      onTapExit = {},
  )
}
