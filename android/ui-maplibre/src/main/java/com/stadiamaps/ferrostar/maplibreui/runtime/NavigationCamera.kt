package com.stadiamaps.ferrostar.maplibreui.runtime

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.models.CameraPadding

sealed class NavigationActivity(val zoom: Double, val pitch: Double) {
  /** The recommended camera configuration for automotive navigation. */
  data object Automotive : NavigationActivity(zoom = 16.0, pitch = 45.0)

  /** The recommended camera configuration for bicycle navigation. */
  data object Bicycle : NavigationActivity(zoom = 18.0, pitch = 45.0)

  /** The recommended camera configuration for pedestrian navigation. */
  data object Pedestrian : NavigationActivity(zoom = 20.0, pitch = 10.0)
}

/**
 * The camera configuration for navigation. This configuration sets the camera to track the user,
 * with a high zoom level and moderate pitch for a 2.5D isometric view. It automatically adjusts the
 * padding based on the screen size and orientation.
 *
 * @param activity The type of activity the camera is being used for.
 * @return The recommended navigation MapViewCamera
 */
@Composable
fun navigationMapViewCamera(
    activity: NavigationActivity = NavigationActivity.Automotive,
): MapViewCamera {
  val screenOrientation = LocalConfiguration.current.orientation
  val start = if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) 0.5f else 0.0f

  val cameraPadding = CameraPadding.fractionOfScreen(start = start, top = 0.5f)

  return MapViewCamera.TrackingUserLocationWithBearing(
      zoom = activity.zoom, pitch = activity.pitch, padding = cameraPadding)
}
