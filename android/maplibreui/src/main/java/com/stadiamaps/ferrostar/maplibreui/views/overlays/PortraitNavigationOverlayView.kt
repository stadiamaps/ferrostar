package com.stadiamaps.ferrostar.maplibreui.views.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.extensions.incrementZoom
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.composeui.views.ArrivalView
import com.stadiamaps.ferrostar.composeui.views.InstructionsView
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
fun PortraitNavigationOverlayView(
    modifier: Modifier,
    camera: MutableState<MapViewCamera>,
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    viewModel: NavigationViewModel,
    config: VisualNavigationViewConfig = VisualNavigationViewConfig.Default(),
    arrivalViewSize: MutableState<DpSize> = remember { mutableStateOf(DpSize.Zero) },
    onTapExit: (() -> Unit)? = null
) {
  val density = LocalDensity.current
  val uiState by viewModel.uiState.collectAsState()

  Column(modifier) {
    uiState.visualInstruction?.let { instructions ->
      InstructionsView(
          instructions, distanceToNextManeuver = uiState.progress?.distanceToNextManeuver)
    }

    NavigatingInnerGridView(
        modifier = Modifier.fillMaxSize().weight(1f).padding(bottom = 16.dp, top = 16.dp),
        showMute = config.showMute,
        isMuted = uiState.isMuted,
        onClickMute = { viewModel.toggleMute() },
        showZoom = config.showZoom,
        onClickZoomIn = { camera.value = camera.value.incrementZoom(1.0) },
        onClickZoomOut = { camera.value = camera.value.incrementZoom(-1.0) },
        showCentering = camera.value.state !is CameraState.TrackingUserLocationWithBearing,
        onClickCenter = { camera.value = navigationCamera },
    )

    uiState.progress?.let { progress ->
      ArrivalView(
          modifier =
              Modifier.onSizeChanged {
                arrivalViewSize.value = density.run { DpSize(it.width.toDp(), it.height.toDp()) }
              },
          progress = progress,
          onTapExit = onTapExit)
    }
  }
}

@Composable
@Preview
fun PortraitNavigationOverlayViewPreview() {
  val viewModel =
      MockNavigationViewModel(
          MutableStateFlow<NavigationUiState>(NavigationUiState.pedestrianExample()).asStateFlow())

  PortraitNavigationOverlayView(
      modifier = Modifier.fillMaxSize(),
      camera = rememberSaveableMapViewCamera(),
      viewModel = viewModel,
      onTapExit = {})
}
