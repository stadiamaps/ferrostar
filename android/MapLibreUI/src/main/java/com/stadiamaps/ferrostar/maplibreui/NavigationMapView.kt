package com.stadiamaps.ferrostar.maplibreui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import com.stadiamaps.ferrostar.composeui.BannerInstructionView
import com.stadiamaps.ferrostar.core.NavigationViewModel
import org.ramani.compose.CameraMotionType
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polyline
import uniffi.ferrostar.VisualInstruction

@Composable
fun NavigationMapView(
    styleUrl: String,
    viewModel: NavigationViewModel,
    bannerContentBuilder: @Composable (VisualInstruction, Double?) -> Unit =
        { instruction, distanceToNextManeuver ->
          BannerInstructionView(instruction, distanceToNextManeuver)
        }
) {
  val uiState = viewModel.uiState.collectAsState()

  Box {
    MapLibre(
        modifier = Modifier.fillMaxSize(),
        styleUrl = styleUrl,
        cameraPosition =
            CameraPosition(
                target =
                    LatLng(
                        uiState.value.snappedLocation.coordinates.lat,
                        uiState.value.snappedLocation.coordinates.lng),
                zoom = 18.0,
                tilt = 45.0,
                bearing = uiState.value.snappedLocation.courseOverGround?.degrees?.toDouble(),
                motionType = CameraMotionType.EASE)) {
          Circle(
              center =
                  LatLng(
                      uiState.value.snappedLocation.coordinates.lat,
                      uiState.value.snappedLocation.coordinates.lng),
              radius = 10f,
              color = "Blue")
          Polyline(
              points = uiState.value.routeGeometry.map { LatLng(it.lat, it.lng) },
              color = "Red",
              lineWidth = 5f)
        }

    uiState.value.visualInstruction?.let {
      bannerContentBuilder(it, uiState.value.distanceToNextManeuver)
    }
  }
}
