package com.stadiamaps.ferrostar.composeui.views.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun PortraitNavigationOverlayView(
    modifier: Modifier,
    viewModel: NavigationViewModel,
    cameraIsTrackingLocation: Boolean,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    views: VisualNavigationViewComponentConfig = VisualNavigationViewComponentConfig.Default(),
    onTapExit: (() -> Unit)? = null,
) {
  val density = LocalDensity.current
  val uiState by viewModel.uiState.collectAsState()
  var instructionsViewSize by remember { mutableStateOf(DpSize.Zero) }

  Column(modifier) {
    uiState.visualInstruction?.let { instructions ->
      InstructionsView(
          instructions,
          modifier =
              Modifier.onSizeChanged {
                instructionsViewSize = density.run { DpSize(it.width.toDp(), it.height.toDp()) }
              },
          remainingSteps = uiState.remainingSteps,
          distanceToNextManeuver = uiState.progress?.distanceToNextManeuver)
    }

    val cameraIsTrackingLocation = camera.value.state is CameraState.TrackingUserLocationWithBearing
    views.instructionsView(uiState)

    NavigatingInnerGridView(
        modifier = Modifier.fillMaxSize().weight(1f).padding(bottom = 16.dp, top = 16.dp),
        showMute = config.showMute,
        isMuted = uiState.isMuted,
        onClickMute = { viewModel.toggleMute() },
        buttonSize = config.buttonSize,
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

    uiState.progress?.let { progress ->
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val currentRoadName =
          // TODO: Fixme
          if (true) {
//            if (cameraIsTrackingLocation) {
              uiState.currentStepRoadName
            } else {
              // Hide the road name view if not tracking the user location
              null
            }
        currentRoadName?.let { roadName -> currentRoadNameView(roadName) }
        TripProgressView(
            modifier =
                Modifier.onSizeChanged {
                  progressViewSize.value = density.run { DpSize(it.width.toDp(), it.height.toDp()) }
                },
            progress = progress,
            onTapExit = onTapExit)
      }
    }
  }
}

@Composable
@Preview
fun PortraitNavigationOverlayViewPreview() {
  val viewModel =
      MockNavigationViewModel(MutableStateFlow(NavigationUiState.pedestrianExample()).asStateFlow())

  PortraitNavigationOverlayView(
      modifier = Modifier.fillMaxSize(),
//      camera = rememberSaveableMapViewCamera(),
      viewModel = viewModel,
      onTapExit = {})
}
