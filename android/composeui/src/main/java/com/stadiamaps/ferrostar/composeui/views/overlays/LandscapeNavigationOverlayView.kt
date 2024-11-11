package com.stadiamaps.ferrostar.composeui.views.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.composeui.config.NavigationViewComponentBuilder
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewComponentConfig
import com.stadiamaps.ferrostar.composeui.views.components.CurrentRoadNameView
import com.stadiamaps.ferrostar.composeui.views.components.InstructionsView
import com.stadiamaps.ferrostar.composeui.views.components.TripProgressView
import com.stadiamaps.ferrostar.composeui.views.components.gridviews.NavigatingInnerGridView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.mock.MockNavigationViewModel
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.theme.DefaultFerrostarTheme
import com.stadiamaps.ferrostar.composeui.theme.FerrostarTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun LandscapeNavigationOverlayView(
  modifier: Modifier,
  viewModel: NavigationViewModel,
  cameraIsTrackingLocation: Boolean,
  theme: FerrostarTheme = DefaultFerrostarTheme,
  config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
  views: NavigationViewComponentBuilder = NavigationViewComponentBuilder.Default(theme),
  progressViewSize: MutableState<DpSize> = remember { mutableStateOf(DpSize.Zero) },
  onTapExit: (() -> Unit)? = null,
) {
  val density = LocalDensity.current
  val uiState by viewModel.uiState.collectAsState()
  var instructionsViewSize by remember { mutableStateOf(DpSize.Zero) }

  Row(modifier) {
    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f)) {
      views.instructionsView(
        Modifier.onSizeChanged {
          instructionsViewSize = density.run { DpSize(it.width.toDp(), it.height.toDp()) }},
        uiState
      )

      Spacer(modifier = Modifier.weight(1f))

      views.progressView(
        Modifier.onSizeChanged {
          progressViewSize.value = density.run { DpSize(it.width.toDp(), it.height.toDp()) }},
        uiState, onTapExit)
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.fillMaxHeight()) {
      NavigatingInnerGridView(
          modifier = Modifier.fillMaxSize(),
          showMute = config.showMute,
          isMuted = uiState.isMuted,
          onClickMute = { viewModel.toggleMute() },
          buttonSize = theme.buttonSize,
          cameraControlState =
              config.cameraControlState(
                  camera,
                  navigationCamera,
                  uiState,
                  NavigationViewMetrics(progressViewSize.value, instructionsViewSize),
              ),
          showZoom = config.showZoom,
          onClickZoomIn = { config.onZoomIn?.invoke() },
          onClickZoomOut = { config.onZoomOut?.invoke() },
          showCentering = !cameraIsTrackingLocation,
          onClickCenter = { config.onCenterLocation?.invoke() },
      )
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
      cameraIsTrackingLocation = false,
      onTapExit = {}
  )
}
