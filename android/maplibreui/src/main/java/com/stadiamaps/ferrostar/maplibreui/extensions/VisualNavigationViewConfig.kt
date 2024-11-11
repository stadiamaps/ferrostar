package com.stadiamaps.ferrostar.maplibreui.extensions

import android.graphics.Camera
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.models.CameraPadding
import com.stadiamaps.ferrostar.composeui.config.CameraControlState
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.core.BoundingBox
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.boundingBox
import com.stadiamaps.ferrostar.maplibreui.NavigationViewMetrics

@Composable
fun VisualNavigationViewConfig.cameraControlState(
  camera: MutableState<MapViewCamera>,
  navigationCamera: MapViewCamera,
  uiState: NavigationUiState,
  cameraIsTrackingLocation: Boolean,
  mapViewInsets: PaddingValues,
  boundingBox: BoundingBox
): CameraControlState {
  return if (!cameraIsTrackingLocation) {
    CameraControlState.ShowRecenter { camera.value = navigationCamera }
  } else {
    CameraControlState.ShowRouteOverview {
      camera.value = MapViewCamera.BoundingBox(
        bounds = LatLngBounds.from(
          boundingBox.north,
          boundingBox.east,
          boundingBox.south,
          boundingBox.west
        ),
        // TODO: Padding w/ compose 0.4.0
      )
    }
  }
}

@Composable
fun VisualNavigationViewConfig.cameraControlState(
    camera: MutableState<MapViewCamera>,
    navigationCamera: MapViewCamera,
    uiState: NavigationUiState,
    navigationViewMetrics: NavigationViewMetrics
): CameraControlState {
  val cameraIsTrackingLocation = camera.value.state is CameraState.TrackingUserLocationWithBearing
  val cameraControlState =
      if (!cameraIsTrackingLocation) {
        CameraControlState.ShowRecenter { camera.value = navigationCamera }
      } else {
        val bbox = uiState.routeGeometry?.boundingBox()
        if (bbox != null) {
          val scale = LocalDensity.current.density
          val progressViewHeight = navigationViewMetrics.progressViewSize.height.value.toDouble()
          val instructionsViewHeight =
              navigationViewMetrics.instructionsViewSize.height.value.toDouble()
          val layoutDirection = LocalLayoutDirection.current

          // Bottom padding must take the recenter button into account
          val bottomPadding = (progressViewHeight + this.buttonSize.height.value + 50) * scale
          // The top padding needs to take the puck into account
          val topPadding = (instructionsViewHeight + 75) * scale
          val (startPadding, endPadding) =
              when (layoutDirection) {
                LayoutDirection.Ltr -> 20.0 * scale to (this.buttonSize.width.value + 50) * scale
                LayoutDirection.Rtl -> (this.buttonSize.width.value + 50) * scale to 20.0 * scale
              }

          CameraControlState.ShowRouteOverview {
            camera.value =
                MapViewCamera.BoundingBox(
                    LatLngBounds.from(bbox.north, bbox.east, bbox.south, bbox.west),
                    padding =
                        CameraPadding(
                            startPadding.toDouble(),
                            topPadding,
                            endPadding.toDouble(),
                            bottomPadding))
          }
        } else {
          CameraControlState.Hidden
        }
      }
  return cameraControlState
}
