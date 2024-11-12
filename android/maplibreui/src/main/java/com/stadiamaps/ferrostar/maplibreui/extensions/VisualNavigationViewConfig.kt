package com.stadiamaps.ferrostar.maplibreui.extensions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.models.CameraPadding
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.models.CameraControlState
import com.stadiamaps.ferrostar.core.BoundingBox

@Composable
fun VisualNavigationViewConfig.cameraControlState(
    camera: MutableState<MapViewCamera>,
    navigationCamera: MapViewCamera,
    mapViewInsets: PaddingValues,
    boundingBox: BoundingBox?
): CameraControlState {
  val cameraIsTrackingLocation = camera.value.state is CameraState.TrackingUserLocationWithBearing
  val cameraPadding = CameraPadding.padding(mapViewInsets)

  return if (!cameraIsTrackingLocation) {
    CameraControlState.ShowRecenter { camera.value = navigationCamera }
  } else {
    if (boundingBox != null) {
      CameraControlState.ShowRouteOverview {
        camera.value =
            MapViewCamera.BoundingBox(
                bounds =
                    LatLngBounds.from(
                        boundingBox.north, boundingBox.east, boundingBox.south, boundingBox.west),
                padding = cameraPadding)
      }
    } else {
      CameraControlState.Hidden
    }
  }
}
