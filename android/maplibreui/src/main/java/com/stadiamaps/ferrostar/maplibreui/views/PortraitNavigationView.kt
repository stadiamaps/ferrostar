package com.stadiamaps.ferrostar.maplibreui.views

import android.view.Gravity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.extensions.incrementZoom
import com.maplibre.compose.ramani.LocationRequestProperties
import com.maplibre.compose.ramani.MapLibreComposable
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.maplibre.compose.settings.AttributionSettings
import com.maplibre.compose.settings.CompassSettings
import com.maplibre.compose.settings.LogoSettings
import com.maplibre.compose.settings.MapControls
import com.maplibre.compose.settings.MarginInsets
import com.stadiamaps.ferrostar.composeui.views.ArrivalView
import com.stadiamaps.ferrostar.composeui.views.InstructionsView
import com.stadiamaps.ferrostar.composeui.views.gridviews.NavigatingInnerGridView
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import com.stadiamaps.ferrostar.core.mock.MockNavigationViewModel
import com.stadiamaps.ferrostar.core.mock.pedestrianExample
import com.stadiamaps.ferrostar.maplibreui.NavigationMapView
import com.stadiamaps.ferrostar.maplibreui.extensions.NavigationDefault
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
fun PortraitNavigationView(
    modifier: Modifier,
    styleUrl: String,
    camera: MutableState<MapViewCamera> = rememberSaveableMapViewCamera(),
    navigationCamera: MapViewCamera = navigationMapViewCamera(),
    viewModel: NavigationViewModel,
    locationRequestProperties: LocationRequestProperties =
        LocationRequestProperties.NavigationDefault(),
    onTapExit: (() -> Unit)? = null,
    content: @Composable @MapLibreComposable() ((State<NavigationUiState>) -> Unit)? = null
) {
  val uiState = viewModel.uiState.collectAsState()

  Box(modifier) {
    NavigationMapView(
        styleUrl,
        MapControls(
            attribution =
                AttributionSettings(
                    gravity = Gravity.BOTTOM or Gravity.END,
                    margins = MarginInsets(end = 270, bottom = 200)),
            compass = CompassSettings(enabled = false),
            logo =
                LogoSettings(
                    gravity = Gravity.BOTTOM or Gravity.END,
                    margins = MarginInsets(end = 32, bottom = 200))),
        camera,
        viewModel,
        locationRequestProperties,
        onMapReadyCallback = { camera.value = navigationCamera },
        content)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      uiState.value.visualInstruction?.let { instructions ->
        InstructionsView(
            instructions, distanceToNextManeuver = uiState.value.progress?.distanceToNextManeuver)
      }

      NavigatingInnerGridView(
          modifier = Modifier.fillMaxSize().weight(1f).padding(bottom = 16.dp),
          onClickZoomIn = { camera.value = camera.value.incrementZoom(1.0) },
          onClickZoomOut = { camera.value = camera.value.incrementZoom(-1.0) },
          showCentering = camera.value.state !is CameraState.TrackingUserLocationWithBearing,
          onClickCenter = { camera.value = navigationCamera })

      uiState.value.progress?.let { progress ->
        ArrivalView(progress = progress, onTapExit = onTapExit)
      }
    }
  }
}

@Preview(device = Devices.PIXEL_5)
@Composable
private fun PortraitNavigationViewPreview() {
  val viewModel =
      MockNavigationViewModel(
          MutableStateFlow<NavigationUiState>(NavigationUiState.pedestrianExample()).asStateFlow())

  PortraitNavigationView(
      Modifier.fillMaxSize(), "https://demotiles.maplibre.org/style.json", viewModel = viewModel)
}
