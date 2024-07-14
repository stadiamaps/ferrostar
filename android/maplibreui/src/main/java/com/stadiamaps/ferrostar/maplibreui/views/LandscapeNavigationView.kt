package com.stadiamaps.ferrostar.maplibreui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.incrementZoom
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.stadiamaps.ferrostar.composeui.views.ArrivalView
import com.stadiamaps.ferrostar.composeui.views.InstructionsView
import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridView
import com.stadiamaps.ferrostar.core.NavigationState
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import kotlinx.coroutines.flow.MutableStateFlow
import uniffi.ferrostar.UserLocation

/**
 * A portrait orientation of the navigation view with instructions, default controls and the
 * navigation map view.
 *
 * @param modifier
 * @param styleUrl
 * @param viewModel
 * @param locationRequestProperties
 */
@Composable
fun LandscapeNavigationView(
    modifier: Modifier,
    styleUrl: String,
    camera: MutableState<MapViewCamera> = rememberSaveableMapViewCamera(),
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    onTapExit: (() -> Unit)? = null,
    content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null
) {
  val uiState = viewModel.uiState.collectAsState()

  Box(modifier) {
    NavigationMapView(styleUrl, camera, viewModel, locationRequestProperties, content)

    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f)) {
        uiState.value.visualInstruction?.let { instructions ->
          InstructionsView(
              instructions, distanceToNextManeuver = uiState.value.progress?.distanceToNextManeuver)
        }

        Spacer(modifier = Modifier.weight(1f))

        uiState.value.progress?.let { progress ->
          ArrivalView(progress = progress, onTapExit = onTapExit)
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.fillMaxHeight()) {
        NavigatingInnerGridView(
            modifier = Modifier.fillMaxSize(),
            onClickZoomIn = { camera.value = camera.value.incrementZoom(1.0) },
            onClickZoomOut = { camera.value = camera.value.incrementZoom(-1.0) },
            showCentering = camera.value.state != CameraState.TrackingUserLocationWithBearing,
            onClickCenter = { camera.value = MapViewCamera.NavigationDefault() })
      }
    }
  }
}

@Preview(
    device =
        "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
private fun LandscapeNavigationViewPreview() {
  val viewModel =
      NavigationViewModel(
          MutableStateFlow<NavigationState>(NavigationState.pedestrianExample()),
          initialUserLocation = UserLocation.pedestrianExample())

  LandscapeNavigationView(
      Modifier.fillMaxSize(), "https://demotiles.maplibre.org/style.json", viewModel = viewModel)
}