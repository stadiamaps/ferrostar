package com.stadiamaps.ferrostar.maplibreui.extensions

import com.maplibre.compose.camera.MapViewCamera

/**
 * The centered camera configuration for navigation. This configuration sets the camera to track the
 * user, with a bearing of 18 degrees and a pitch of 45 degrees.
 *
 * @return The navigation MapViewCamera
 */
fun MapViewCamera.Companion.NavigationCentered(): MapViewCamera {
  return MapViewCamera.TrackingUserLocationWithBearing(zoom = 18.0, pitch = 45.0)
}
