package com.stadiamaps.ferrostar.maplibreui.views.overlays

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.extensions.incrementZoom
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.composeui.views.CurrentRoadNameView
import com.stadiamaps.ferrostar.composeui.views.InstructionsView
import com.stadiamaps.ferrostar.composeui.views.TripProgressView
import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.mock.MockNavigationViewModel
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
import com.stadiamaps.ferrostar.maplibreui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun LandscapeNavigationOverlayView(
    modifier: Modifier,
    camera: MutableState<MapViewCamera>,
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    viewModel: NavigationViewModel,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    currentRoadNameView: @Composable (String?) -> Unit = { roadName ->
      if (roadName != null) {
        CurrentRoadNameView(roadName)
        Spacer(modifier = Modifier.height(8.dp))
      }
    },
    onTapExit: (() -> Unit)? = null,
) {
  val uiState by viewModel.uiState.collectAsState()
  val cameraIsTrackingLocation = camera.value.state is CameraState.TrackingUserLocationWithBearing

  Row(modifier) {
    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f)) {
      uiState.visualInstruction?.let { instructions ->
        InstructionsView(
            instructions,
            remainingSteps = uiState.remainingSteps,
            distanceToNextManeuver = uiState.progress?.distanceToNextManeuver)
      }

      Spacer(modifier = Modifier.weight(1f))

      uiState.progress?.let { progress ->
        TripProgressView(progress = progress, onTapExit = onTapExit)
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.fillMaxHeight()) {
      NavigatingInnerGridView(
          modifier = Modifier.fillMaxSize(),
          showMute = config.showMute,
          isMuted = uiState.isMuted,
          onClickMute = { viewModel.toggleMute() },
          showZoom = config.showZoom,
          onClickZoomIn = { camera.value = camera.value.incrementZoom(1.0) },
          onClickZoomOut = { camera.value = camera.value.incrementZoom(-1.0) },
          showCentering = !cameraIsTrackingLocation,
          onClickCenter = { camera.value = navigationCamera })
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
      camera = rememberSaveableMapViewCamera(),
      viewModel = viewModel,
      onTapExit = {})
}
