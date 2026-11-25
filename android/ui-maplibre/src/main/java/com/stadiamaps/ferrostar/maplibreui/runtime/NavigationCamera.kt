package com.stadiamaps.ferrostar.maplibreui.runtime

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.core.BoundingBox
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapState
import org.maplibre.compose.util.toDpPadding
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

  fun tracksLocation(): Boolean = this == FOLLOW_USER || this == FOLLOW_USER_WITH_BEARING
}

data class NavigationCameraOptions(
    val browsingZoom: Double,
    val navigationZoom: Double,
    val navigationTilt: Double,
    val browsingPadding: PaddingValues,
    val navigationPadding: PaddingValues,
) {
  /**
   * Returns the camera that follows the user at [target] while browsing.
   *
   * [layoutDirection] resolves the start and end edges of [browsingPadding].
   */
  fun browsingUser(
      target: Position,
      layoutDirection: LayoutDirection = LayoutDirection.Ltr,
  ): CameraPosition =
      CameraPosition(
          center = target,
          zoom = browsingZoom,
          pitch = 0.0,
          bearing = 0.0,
          padding = browsingPadding.toDpPadding(layoutDirection),
      )

  /**
   * Returns the camera that follows the user at [target] with [bearing] while navigating.
   *
   * [layoutDirection] resolves the start and end edges of [navigationPadding].
   */
  fun navigatingUser(
      target: Position,
      bearing: Double = 0.0,
      layoutDirection: LayoutDirection = LayoutDirection.Ltr,
  ): CameraPosition =
      CameraPosition(
          center = target,
          zoom = navigationZoom,
          pitch = navigationTilt,
          bearing = bearing,
          padding = navigationPadding.toDpPadding(layoutDirection),
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

internal fun NavigationCameraOptions.withNavigationBottomInset(
    bottomInset: Dp,
    screenHeight: Dp,
    layoutDirection: LayoutDirection,
    clearance: Dp = 24.dp,
): NavigationCameraOptions =
    navigationPadding.calculateBottomPadding().let { baseBottomPadding ->
      val topPadding = navigationPadding.calculateTopPadding()
      val currentTargetY = (screenHeight + topPadding - baseBottomPadding) / 2
      val lowestVisibleTargetY = screenHeight - bottomInset - clearance
      // MapLibre centers the target in the padded viewport. Moving the bottom edge up by X only
      // moves the center by X / 2, so double the needed target shift when converting it to padding.
      val extraBottomPadding = maxOf(0.dp, (currentTargetY - lowestVisibleTargetY) * 2)

      copy(
          navigationPadding =
              PaddingValues(
                  start = navigationPadding.calculateStartPadding(layoutDirection),
                  top = topPadding,
                  end = navigationPadding.calculateEndPadding(layoutDirection),
                  bottom = baseBottomPadding + extraBottomPadding,
              )
      )
    }

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

/** Changes the zoom of the camera by [delta] without animation. The zoom does not go below zero. */
fun MapState.incrementZoom(delta: Double) {
  setCameraPosition(cameraPosition.copy(zoom = (cameraPosition.zoom + delta).coerceAtLeast(0.0)))
}

internal fun NavigationMapState.templateFollowingCameraPosition(
    target: Position,
    bearing: Double?,
): CameraPosition =
    when (cameraMode) {
      NavigationCameraMode.FOLLOW_USER ->
          navigationCameraOptions.browsingUser(target, layoutDirection)
      NavigationCameraMode.FOLLOW_USER_WITH_BEARING ->
          navigationCameraOptions.navigatingUser(
              target = target,
              bearing = bearing ?: cameraPosition.bearing,
              layoutDirection = layoutDirection,
          )
      else -> cameraPosition
    }

internal fun NavigationMapState.trackingFollowingCameraPosition(
    target: Position,
    bearing: Double?,
): CameraPosition =
    when (cameraMode) {
      NavigationCameraMode.FOLLOW_USER ->
          cameraPosition.copy(
              center = target,
              pitch = 0.0,
              bearing = 0.0,
              padding = navigationCameraOptions.browsingPadding.toDpPadding(layoutDirection),
          )
      NavigationCameraMode.FOLLOW_USER_WITH_BEARING ->
          cameraPosition.copy(
              center = target,
              pitch = navigationCameraOptions.navigationTilt,
              bearing = bearing ?: cameraPosition.bearing,
              padding = navigationCameraOptions.navigationPadding.toDpPadding(layoutDirection),
          )
      else -> cameraPosition
    }
