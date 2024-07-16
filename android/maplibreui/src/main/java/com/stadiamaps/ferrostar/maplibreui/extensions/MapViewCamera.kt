package com.stadiamaps.ferrostar.maplibreui.extensions

import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.models.CameraPadding

/**
 * The centered camera configuration for navigation. This configuration sets the camera to track the
 * user, with a bearing of 18 degrees and a pitch of 45 degrees.
 *
 * @return The navigation MapViewCamera
 */
fun MapViewCamera.Companion.NavigationCentered(): MapViewCamera {
  return MapViewCamera.TrackingUserLocationWithBearing(zoom = 18.0, pitch = 45.0)
}

/**
 * The landscape camera configuration for navigation. This configuration sets the camera to track
 * the user, with a bearing of 18 degrees and a pitch of 45 degrees as well as a padding to ensure
 * the user puck is centered at the bottom of the screen.
 *
 * @return The navigation MapViewCamera
 */
fun MapViewCamera.Companion.NavigationLandscape(): MapViewCamera {
  return MapViewCamera.TrackingUserLocationWithBearing(
      zoom = 18.0, pitch = 45.0, padding = CameraPadding.NavigationLandscape())
}

/**
 * The portrait camera configuration for navigation. This configuration sets the camera to track the
 * user, with a bearing of 18 degrees and a pitch of 45 degrees as well as a padding to ensure the
 * user puck is centered at the bottom end (right) of the screen.
 *
 * @return The navigation MapViewCamera
 */
fun MapViewCamera.Companion.NavigationPortrait(): MapViewCamera {
  return MapViewCamera.TrackingUserLocationWithBearing(
      zoom = 18.0, pitch = 45.0, padding = CameraPadding.NavigationPortrait())
}
