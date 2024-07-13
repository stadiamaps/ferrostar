package com.stadiamaps.ferrostar.maplibreui.extensions

import com.maplibre.compose.camera.CameraPitch
import com.maplibre.compose.camera.MapViewCamera

/**
 * The default camera configuration for navigation. This configuration sets the camera to track the
 * user, with a bearing of 18 degrees and a pitch of 45 degrees.
 *
 * @return The navigation MapViewCamera
 */
fun MapViewCamera.Companion.NavigationDefault(): MapViewCamera {
  // FIXME: Pitch is not being propagated
  return MapViewCamera.TrackingUserLocationWithBearing(18.0, CameraPitch.Fixed(45.0))
}
