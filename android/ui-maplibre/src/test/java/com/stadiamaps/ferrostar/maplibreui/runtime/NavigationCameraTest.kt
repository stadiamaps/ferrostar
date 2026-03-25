package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import com.stadiamaps.ferrostar.core.BoundingBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

class NavigationCameraTest {
  @Test
  fun defaultNavigationCameraModeUsesBearingWhenNavigating() {
    assertEquals(
        NavigationCameraMode.FOLLOW_USER_WITH_BEARING,
        defaultNavigationCameraMode(isNavigating = true),
    )
    assertEquals(
        NavigationCameraMode.FOLLOW_USER,
        defaultNavigationCameraMode(isNavigating = false),
    )
  }

  @Test
  fun tracksLocationOnlyForFollowingModes() {
    assertTrue(NavigationCameraMode.FOLLOW_USER.tracksLocation())
    assertTrue(NavigationCameraMode.FOLLOW_USER_WITH_BEARING.tracksLocation())
    assertFalse(NavigationCameraMode.OVERVIEW.tracksLocation())
    assertFalse(NavigationCameraMode.FREE.tracksLocation())
  }

  @Test
  fun mapLibreBoundingBoxPreservesEdges() {
    val boundingBox = BoundingBox(north = 48.5, east = 16.7, south = 48.1, west = 16.2)

    val converted = boundingBox.toMapLibreBoundingBox()

    assertEquals(16.2, converted.west, 0.0)
    assertEquals(48.1, converted.south, 0.0)
    assertEquals(16.7, converted.east, 0.0)
    assertEquals(48.5, converted.north, 0.0)
  }

  @Test
  fun browsingCameraIsTopDownAndNorthUp() {
    val options =
        NavigationCameraOptions(
            browsingZoom = 16.0,
            navigationZoom = 16.0,
            navigationTilt = 45.0,
            browsingPadding = PaddingValues(),
            navigationPadding = PaddingValues(),
        )

    val browsing = options.browsingUser(Position(16.37, 48.21))
    val navigating = options.navigatingUser(Position(16.37, 48.21), bearing = 87.0)

    assertEquals(0.0, browsing.tilt, 0.0)
    assertEquals(0.0, browsing.bearing, 0.0)
    assertEquals(45.0, navigating.tilt, 0.0)
    assertEquals(87.0, navigating.bearing, 0.0)
  }

  @Test
  fun trackingCameraPreservesCurrentZoomInBrowsingMode() {
    val state =
        createState(
            cameraMode = NavigationCameraMode.FOLLOW_USER,
            cameraPosition = CameraPosition(zoom = 13.5),
        )

    val position = state.trackingFollowingCameraPosition(Position(16.37, 48.21), bearing = null)

    assertEquals(13.5, position.zoom, 0.0)
    assertEquals(0.0, position.bearing, 0.0)
    assertEquals(0.0, position.tilt, 0.0)
  }

  @Test
  fun trackingCameraPreservesCurrentZoomInNavigationMode() {
    val state =
        createState(
            cameraMode = NavigationCameraMode.FOLLOW_USER_WITH_BEARING,
            cameraPosition = CameraPosition(zoom = 14.5),
        )

    val position =
        state.trackingFollowingCameraPosition(Position(16.37, 48.21), bearing = 87.0)

    assertEquals(14.5, position.zoom, 0.0)
    assertEquals(87.0, position.bearing, 0.0)
    assertEquals(45.0, position.tilt, 0.0)
  }

  @Test
  fun templateCameraUsesConfiguredZoomInBrowsingMode() {
    val state =
        createState(
            cameraMode = NavigationCameraMode.FOLLOW_USER,
            cameraPosition = CameraPosition(zoom = 13.5),
        )

    val position = state.templateFollowingCameraPosition(Position(16.37, 48.21), bearing = null)

    assertEquals(16.0, position.zoom, 0.0)
  }

  @Test
  fun templateCameraUsesConfiguredZoomInNavigationMode() {
    val state =
        createState(
            cameraMode = NavigationCameraMode.FOLLOW_USER_WITH_BEARING,
            cameraPosition = CameraPosition(zoom = 13.5),
        )

    val position = state.templateFollowingCameraPosition(Position(16.37, 48.21), bearing = 87.0)

    assertEquals(16.0, position.zoom, 0.0)
    assertEquals(87.0, position.bearing, 0.0)
  }

  private fun createState(
      cameraMode: NavigationCameraMode,
      cameraPosition: CameraPosition,
  ): NavigationMapState =
      NavigationMapState(
          cameraState = CameraState(cameraPosition),
          initialCameraMode = cameraMode,
          navigationCameraOptions =
              NavigationCameraOptions(
                  browsingZoom = 16.0,
                  navigationZoom = 16.0,
                  navigationTilt = 45.0,
                  browsingPadding = PaddingValues(),
                  navigationPadding = PaddingValues(),
              ),
          coroutineScope = CoroutineScope(
              Job() + Dispatchers.Unconfined,
          ),
      )
}
