package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.maplibre.compose.location.Location

/**
 * Mutable holder for tracking state that does NOT use Compose State,
 * so writes during composition don't trigger recomposition.
 */
private class TrackingState {
  var hadLocation = false
  var lastMode: NavigationCameraMode? = null
}

/**
 * Sets the camera position synchronously during composition so that
 * camera and puck update on the same frame.
 *
 * On the first location or after a mode change, the camera snaps to
 * the template position (setting zoom/tilt from [NavigationCameraOptions]).
 * On subsequent updates it preserves zoom/tilt and only updates target/bearing.
 */
@Composable
internal fun TrackingCameraEffect(
    navigationMapState: NavigationMapState,
    userLocation: Location?,
) {
  val state = remember { TrackingState() }
  val cameraState = navigationMapState.cameraState

  if (userLocation != null && navigationMapState.isTrackingUser) {
    val shouldSnap = !state.hadLocation || state.lastMode != navigationMapState.cameraMode
    if (shouldSnap) {
      navigationMapState.snapTrackingCameraToUserLocation(userLocation)
    } else {
      cameraState.position =
          navigationMapState.trackingFollowingCameraPosition(
              target = userLocation.position,
              bearing = userLocation.bearing,
          )
    }
    state.hadLocation = true
    state.lastMode = navigationMapState.cameraMode
  } else {
    state.hadLocation = false
    state.lastMode = navigationMapState.cameraMode
  }
}

internal fun NavigationMapState.snapTrackingCameraToUserLocation(userLocation: Location) {
  cameraState.position =
      templateFollowingCameraPosition(
          target = userLocation.position,
          bearing = userLocation.bearing,
      )
}
