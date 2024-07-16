package com.stadiamaps.ferrostar.maplibreui.extensions

import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.models.CameraPadding

fun MapViewCamera.Companion.NavigationCentered(): MapViewCamera {
  // FIXME: Pitch is not being propagated
  return MapViewCamera.TrackingUserLocationWithBearing(zoom = 18.0, pitch = 45.0)
}

/**
 * The default camera configuration for navigation. This configuration sets the camera to track the
 * user, with a bearing of 18 degrees and a pitch of 45 degrees.
 *
 * @return The navigation MapViewCamera
 */
fun MapViewCamera.Companion.NavigationLandscape(): MapViewCamera {
  // FIXME: Pitch is not being propagated
  return MapViewCamera.TrackingUserLocationWithBearing(
      zoom = 18.0, pitch = 45.0, padding = CameraPadding.NavigationLandscape())
}

fun MapViewCamera.Companion.NavigationPortrait(): MapViewCamera {
  return MapViewCamera.TrackingUserLocationWithBearing(
      zoom = 18.0, pitch = 45.0, padding = CameraPadding.NavigationPortrait())
}
