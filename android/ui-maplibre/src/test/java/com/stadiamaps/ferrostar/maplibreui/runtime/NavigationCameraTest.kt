package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.core.BoundingBox
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapState
import org.maplibre.compose.util.DpPadding
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

    assertEquals(0.0, browsing.pitch, 0.0)
    assertEquals(0.0, browsing.bearing, 0.0)
    assertEquals(45.0, navigating.pitch, 0.0)
    assertEquals(87.0, navigating.bearing, 0.0)
  }

  @Test
  fun cameraTemplatesResolveStartAndEndPaddingWithTheLayoutDirection() {
    val options =
        NavigationCameraOptions(
            browsingZoom = 16.0,
            navigationZoom = 16.0,
            navigationTilt = 45.0,
            browsingPadding = PaddingValues(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp),
            navigationPadding = PaddingValues(start = 5.dp, top = 6.dp, end = 7.dp, bottom = 8.dp),
        )
    val target = Position(16.37, 48.21)

    assertEquals(
        DpPadding(left = 1.dp, top = 2.dp, right = 3.dp, bottom = 4.dp),
        options.browsingUser(target, LayoutDirection.Ltr).padding,
    )
    assertEquals(
        DpPadding(left = 3.dp, top = 2.dp, right = 1.dp, bottom = 4.dp),
        options.browsingUser(target, LayoutDirection.Rtl).padding,
    )
    assertEquals(
        DpPadding(left = 7.dp, top = 6.dp, right = 5.dp, bottom = 8.dp),
        options.navigatingUser(target, layoutDirection = LayoutDirection.Rtl).padding,
    )
  }

  @Test
  fun trackingCameraUsesTheStateLayoutDirection() {
    val state =
        createState(
            cameraMode = NavigationCameraMode.FOLLOW_USER_WITH_BEARING,
            cameraPosition = CameraPosition(zoom = 14.5),
            navigationPadding = PaddingValues(start = 40.dp),
        )
    state.layoutDirection = LayoutDirection.Rtl

    val position = state.trackingFollowingCameraPosition(Position(16.37, 48.21), bearing = 87.0)

    assertEquals(DpPadding(right = 40.dp), position.padding)
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
    assertEquals(0.0, position.pitch, 0.0)
  }

  @Test
  fun trackingCameraPreservesCurrentZoomInNavigationMode() {
    val state =
        createState(
            cameraMode = NavigationCameraMode.FOLLOW_USER_WITH_BEARING,
            cameraPosition = CameraPosition(zoom = 14.5),
        )

    val position = state.trackingFollowingCameraPosition(Position(16.37, 48.21), bearing = 87.0)

    assertEquals(14.5, position.zoom, 0.0)
    assertEquals(87.0, position.bearing, 0.0)
    assertEquals(45.0, position.pitch, 0.0)
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
      navigationPadding: PaddingValues = PaddingValues(),
  ): NavigationMapState =
      NavigationMapState(
              cameraModeState = mutableStateOf(cameraMode),
              retainedCameraPositionState = mutableStateOf(CameraPosition()),
              navigationCameraOptions =
                  NavigationCameraOptions(
                      browsingZoom = 16.0,
                      navigationZoom = 16.0,
                      navigationTilt = 45.0,
                      browsingPadding = PaddingValues(),
                      navigationPadding = navigationPadding,
                  ),
              coroutineScope =
                  CoroutineScope(
                      Job() + Dispatchers.Unconfined,
                  ),
          )
          .also { state ->
            state.attach(
                mockk<MapState> { every { this@mockk.cameraPosition } returns cameraPosition }
            )
          }
}
