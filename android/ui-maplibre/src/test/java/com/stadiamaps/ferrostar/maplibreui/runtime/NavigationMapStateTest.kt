package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import com.stadiamaps.ferrostar.core.BoundingBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState

class NavigationMapStateTest {
  private val testScope = CoroutineScope(Job() + Dispatchers.Unconfined)

  @Test
  fun recenterUsesBrowsingModeWhenNotNavigating() {
    val state = createState(initialCameraMode = NavigationCameraMode.FREE)

    state.recenter(isNavigating = false)

    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
    assertTrue(state.isTrackingUser)
  }

  @Test
  fun recenterUsesBearingModeWhenNavigating() {
    val state = createState(initialCameraMode = NavigationCameraMode.FREE)

    state.recenter(isNavigating = true)

    assertEquals(NavigationCameraMode.FOLLOW_USER_WITH_BEARING, state.cameraMode)
    assertTrue(state.isTrackingUser)
  }

  @Test
  fun zoomHelpersAdjustCameraZoom() {
    val cameraState = CameraState(CameraPosition(zoom = 10.0))
    val state = createState(cameraState = cameraState)

    state.zoomIn()
    state.zoomOut(delta = 2.0)

    assertEquals(9.0, cameraState.position.zoom, 0.0)
  }

  @Test
  fun routeOverviewSwitchesToOverviewMode() {
    val state = createState(initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.showRouteOverview(
        boundingBox = BoundingBox(north = 48.5, east = 16.7, south = 48.1, west = 16.2),
        paddingValues = PaddingValues(),
    )

    assertEquals(NavigationCameraMode.OVERVIEW, state.cameraMode)
    assertFalse(state.isTrackingUser)
  }

  @Test
  fun navigationCameraOptionsRemainMutable() {
    val options =
        NavigationCameraOptions(
            browsingZoom = 12.0,
            navigationZoom = 15.0,
            navigationTilt = 30.0,
            browsingPadding = PaddingValues(),
            navigationPadding = PaddingValues(),
        )
    val state = createState()

    state.navigationCameraOptions = options

    assertSame(options, state.navigationCameraOptions)
  }

  @Test
  fun zeroOverviewAnimationDurationIsNormalized() {
    assertEquals(1.milliseconds, normalizeOverviewAnimationDuration(0.milliseconds))
    assertEquals(1.milliseconds, normalizeOverviewAnimationDuration((-50).milliseconds))
    assertEquals(300.milliseconds, normalizeOverviewAnimationDuration(300.milliseconds))
  }

  private fun createState(
      cameraState: CameraState = CameraState(CameraPosition()),
      initialCameraMode: NavigationCameraMode = defaultNavigationCameraMode(isNavigating = false),
  ): NavigationMapState =
      NavigationMapState(
          cameraState = cameraState,
          initialCameraMode = initialCameraMode,
          navigationCameraOptions =
              NavigationCameraOptions(
                  browsingZoom = 16.0,
                  navigationZoom = 16.0,
                  navigationTilt = 45.0,
                  browsingPadding = PaddingValues(),
                  navigationPadding = PaddingValues(),
              ),
          coroutineScope = testScope,
      )
}
