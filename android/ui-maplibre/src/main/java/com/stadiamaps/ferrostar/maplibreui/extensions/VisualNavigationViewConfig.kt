package com.stadiamaps.ferrostar.maplibreui.extensions

import androidx.compose.foundation.layout.PaddingValues
import com.stadiamaps.ferrostar.composeui.config.VisualNavigationViewConfig
import com.stadiamaps.ferrostar.composeui.models.CameraControlState
import com.stadiamaps.ferrostar.core.BoundingBox
import com.stadiamaps.ferrostar.maplibreui.runtime.NavigationMapState

fun VisualNavigationViewConfig.cameraControlState(
    navigationMapState: NavigationMapState,
    isNavigating: Boolean,
    mapViewInsets: PaddingValues,
    boundingBox: BoundingBox?,
): CameraControlState {
  return if (!navigationMapState.isTrackingUser) {
    CameraControlState.ShowRecenter { navigationMapState.recenter(isNavigating) }
  } else if (boundingBox != null) {
    CameraControlState.ShowRouteOverview {
      navigationMapState.showRouteOverview(
          boundingBox = boundingBox,
          paddingValues = mapViewInsets,
      )
    }
  } else {
    CameraControlState.Hidden
  }
}
