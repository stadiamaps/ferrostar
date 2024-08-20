package com.stadiamaps.ferrostar.maplibreui.extensions

import com.maplibre.compose.camera.MapViewCamera

/**
 * The centered camera configuration for navigation. This configuration sets the camera to track the
 * user, with a zoom of 16 degrees and a pitch of 45 degrees.
 *
 * @return The navigation MapViewCamera
 */
fun MapViewCamera.Companion.NavigationCentered(): MapViewCamera {
  return MapViewCamera.TrackingUserLocationWithBearing(zoom = 16.0, pitch = 45.0)
}
