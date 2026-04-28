package com.stadiamaps.ferrostar.maplibreui.runtime

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.core.BoundingBox
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

sealed class NavigationActivity(val zoom: Double, val tilt: Double) {
  /** The recommended camera configuration for automotive navigation. */
  data object Automotive : NavigationActivity(zoom = 16.0, tilt = 45.0)

  /** The recommended camera configuration for bicycle navigation. */
  data object Bicycle : NavigationActivity(zoom = 18.0, tilt = 45.0)

  /** The recommended camera configuration for pedestrian navigation. */
  data object Pedestrian : NavigationActivity(zoom = 20.0, tilt = 10.0)
}

enum class NavigationCameraMode {
  FOLLOW_USER,
  FOLLOW_USER_WITH_BEARING,
  OVERVIEW,
  FREE;

  fun tracksLocation(): Boolean =
      this == FOLLOW_USER || this == FOLLOW_USER_WITH_BEARING
}

data class NavigationCameraOptions(
    val browsingZoom: Double,
    val navigationZoom: Double,
    val navigationTilt: Double,
    val browsingPadding: PaddingValues,
    val navigationPadding: PaddingValues,
) {
  fun browsingUser(target: Position): CameraPosition =
      CameraPosition(
          target = target,
          zoom = browsingZoom,
          tilt = 0.0,
          bearing = 0.0,
          padding = browsingPadding,
      )

  fun navigatingUser(target: Position, bearing: Double = 0.0): CameraPosition =
      CameraPosition(
          target = target,
          zoom = navigationZoom,
          tilt = navigationTilt,
          bearing = bearing,
          padding = navigationPadding,
      )
}

/**
 * Returns the recommended camera configuration for navigation. The default keeps the user's
 * location lower in the viewport to leave room for instructions and overlays.
 */
@Composable
fun navigationCameraOptions(
    activity: NavigationActivity = NavigationActivity.Automotive,
): NavigationCameraOptions {
  val configuration = LocalConfiguration.current

  return NavigationCameraOptions(
      browsingZoom = activity.zoom,
      navigationZoom = activity.zoom,
      navigationTilt = activity.tilt,
      browsingPadding = PaddingValues(0.dp),
      navigationPadding =
          navigationPaddingForScreen(
              orientation = configuration.orientation,
              screenWidthDp = configuration.screenWidthDp,
              screenHeightDp = configuration.screenHeightDp,
          ),
  )
}

internal fun navigationPaddingForScreen(
    orientation: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
): PaddingValues =
    PaddingValues(
        start =
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
              (screenWidthDp * 0.5f).dp
            } else {
              0.dp
            },
        top = (screenHeightDp * 0.5f).dp,
    )

fun defaultNavigationCameraMode(isNavigating: Boolean): NavigationCameraMode =
    if (isNavigating) {
      NavigationCameraMode.FOLLOW_USER_WITH_BEARING
    } else {
      NavigationCameraMode.FOLLOW_USER
    }

fun BoundingBox.toMapLibreBoundingBox(): org.maplibre.spatialk.geojson.BoundingBox =
    org.maplibre.spatialk.geojson.BoundingBox(
        west = west,
        south = south,
        east = east,
        north = north,
    )

fun CameraState.incrementZoom(delta: Double) {
  position = position.copy(zoom = (position.zoom + delta).coerceAtLeast(0.0))
}

internal fun NavigationMapState.templateFollowingCameraPosition(
    target: Position,
    bearing: Double?,
): CameraPosition =
    when (cameraMode) {
      NavigationCameraMode.FOLLOW_USER -> navigationCameraOptions.browsingUser(target)
      NavigationCameraMode.FOLLOW_USER_WITH_BEARING ->
          navigationCameraOptions.navigatingUser(
              target = target,
              bearing = bearing ?: cameraState.position.bearing,
          )
      else -> cameraState.position
    }

internal fun NavigationMapState.trackingFollowingCameraPosition(
    target: Position,
    bearing: Double?,
): CameraPosition =
    when (cameraMode) {
      NavigationCameraMode.FOLLOW_USER ->
          cameraState.position.copy(
              target = target,
              tilt = 0.0,
              bearing = 0.0,
              padding = navigationCameraOptions.browsingPadding,
          )
      NavigationCameraMode.FOLLOW_USER_WITH_BEARING ->
          cameraState.position.copy(
              target = target,
              tilt = navigationCameraOptions.navigationTilt,
              bearing = bearing ?: cameraState.position.bearing,
              padding = navigationCameraOptions.navigationPadding,
          )
      else -> cameraState.position
    }
