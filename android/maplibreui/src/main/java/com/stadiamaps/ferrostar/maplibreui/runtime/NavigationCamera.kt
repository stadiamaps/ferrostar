package com.stadiamaps.ferrostar.maplibreui.runtime

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.cameraPaddingFractionOfScreen

/**
 * The camera configuration for navigation. This configuration sets the camera to track the
 * user, with a bearing of 18 degrees and a pitch of 45 degrees. It automatically adjusts the
 * padding based on the screen size and orientation.
 *
 * @param zoom The zoom level of the camera.
 * @param pitch The pitch of the camera.
 * @return The recommended navigation MapViewCamera
 */
@Composable
fun navigationMapViewCamera(zoom: Double = 18.0, pitch: Double = 45.0): MapViewCamera {
  val screenOrientation = LocalConfiguration.current.orientation
  val start = if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) 0.8f else 0.0f

  val cameraPadding = cameraPaddingFractionOfScreen(start = start, top= 1.4f)

  return MapViewCamera.TrackingUserLocationWithBearing(
    zoom = zoom,
    pitch = pitch,
    padding = cameraPadding
  )
}