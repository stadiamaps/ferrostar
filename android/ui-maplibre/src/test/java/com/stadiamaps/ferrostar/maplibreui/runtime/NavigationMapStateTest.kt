package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.core.BoundingBox
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.compose.camera.CameraProjection
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

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
  fun zoomHelpersAnimateCameraZoom() {
    val cameraState = mockCameraState(initialPosition = CameraPosition(zoom = 10.0))
    val state = createState(cameraState = cameraState, initialCameraMode = NavigationCameraMode.FREE)

    state.zoomIn()
    state.zoomOut(delta = 2.0)

    coVerify {
      cameraState.animateTo(
          finalPosition = match { it.zoom == 11.0 },
          duration = DEFAULT_ZOOM_ANIMATION_DURATION,
      )
    }
    coVerify {
      cameraState.animateTo(
          finalPosition = match { it.zoom == 9.0 },
          duration = DEFAULT_ZOOM_ANIMATION_DURATION,
      )
    }
  }

  @Test
  fun panBySwitchesToFreeCameraAndUpdatesTarget() {
    val initialPosition = CameraPosition(target = Position(16.0, 48.0), zoom = 10.0)
    val updatedTarget = Position(16.1, 48.2)
    val projection = mockk<CameraProjection>()
    val cameraState = mockCameraState(initialPosition = initialPosition, projection = projection)
    val state = createState(cameraState = cameraState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    every { projection.screenLocationFromPosition(initialPosition.target) } returns DpOffset(100.dp, 200.dp)
    every { projection.positionFromScreenLocation(DpOffset(120.dp, 180.dp)) } returns updatedTarget

    state.panBy(DpOffset(20.dp, (-20).dp))

    assertEquals(NavigationCameraMode.FREE, state.cameraMode)
    assertEquals(updatedTarget, cameraState.position.target)
  }

  @Test
  fun panByIsNoOpWithoutProjection() {
    val initialPosition = CameraPosition(target = Position(16.0, 48.0), zoom = 10.0)
    val cameraState = CameraState(initialPosition)
    val state = createState(cameraState = cameraState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.panBy(DpOffset(5.dp, 7.dp))

    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
    assertEquals(initialPosition, cameraState.position)
  }

  @Test
  fun flingBySwitchesToFreeCameraAndAnimatesTarget() {
    val initialPosition = CameraPosition(target = Position(16.0, 48.0), zoom = 10.0)
    val updatedTarget = Position(16.3, 48.4)
    val projection = mockk<CameraProjection>()
    val cameraState = mockCameraState(initialPosition = initialPosition, projection = projection)
    val state = createState(cameraState = cameraState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    every { projection.screenLocationFromPosition(initialPosition.target) } returns DpOffset(50.dp, 80.dp)
    every { projection.positionFromScreenLocation(DpOffset(65.dp, 100.dp)) } returns updatedTarget

    state.flingBy(DpOffset(15.dp, 20.dp), duration = 120.milliseconds)

    assertEquals(NavigationCameraMode.FREE, state.cameraMode)
    coVerify {
      cameraState.animateTo(
          finalPosition = match { it.target == updatedTarget },
          duration = 120.milliseconds,
      )
    }
  }

  @Test
  fun flingByIsNoOpWithoutProjection() {
    val initialPosition = CameraPosition(target = Position(16.0, 48.0), zoom = 10.0)
    val cameraState = mockk<CameraState>(relaxed = true)
    every { cameraState.projection } returns null
    every { cameraState.position } returns initialPosition
    coEvery { cameraState.animateTo(any<CameraPosition>(), any<Duration>()) } returns Unit
    val state = createState(cameraState = cameraState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.flingBy(DpOffset(15.dp, 20.dp), duration = 120.milliseconds)

    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
    coVerify(exactly = 0) { cameraState.animateTo(any<CameraPosition>(), any<Duration>()) }
  }

  @Test
  fun scaleByIgnoresNonPositiveFactors() {
    val cameraState = CameraState(CameraPosition(zoom = 10.0))
    val state = createState(cameraState = cameraState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.scaleBy(0f)
    state.scaleBy(-1f)

    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
    assertEquals(10.0, cameraState.position.zoom, 0.0)
  }

  @Test
  fun scaleByUsesLogBase2ZoomDelta() {
    val cameraState = CameraState(CameraPosition(zoom = 10.0))
    val state = createState(cameraState = cameraState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.scaleBy(2f)
    assertEquals(NavigationCameraMode.FREE, state.cameraMode)
    assertEquals(11.0, cameraState.position.zoom, 0.0)

    state.scaleBy(0.5f)
    assertEquals(10.0, cameraState.position.zoom, 0.0001)
  }

  @Test
  fun scaleByClampsZoomAtZero() {
    val cameraState = CameraState(CameraPosition(zoom = 0.25))
    val state = createState(cameraState = cameraState)

    state.scaleBy(0.1f)

    assertEquals(0.0, cameraState.position.zoom, 0.0)
  }

  @Test
  fun recenterUsesBrowsingTemplateZoomAfterFreeCamera() {
    val cameraState = CameraState(CameraPosition(zoom = 10.0))
    val state =
        createState(
            cameraState = cameraState,
            initialCameraMode = NavigationCameraMode.FREE,
        )

    state.recenter(isNavigating = false)

    val position =
        state.templateFollowingCameraPosition(
            target = CameraPosition().target,
            bearing = null,
        )

    assertEquals(16.0, position.zoom, 0.0)
    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
  }

  @Test
  fun recenterUsesNavigationTemplateZoomAfterOverview() {
    val cameraState = CameraState(CameraPosition(zoom = 10.0))
    val state =
        createState(
            cameraState = cameraState,
            initialCameraMode = NavigationCameraMode.OVERVIEW,
        )

    state.recenter(isNavigating = true)

    val position =
        state.templateFollowingCameraPosition(
            target = CameraPosition().target,
            bearing = 87.0,
        )

    assertEquals(16.0, position.zoom, 0.0)
    assertEquals(87.0, position.bearing, 0.0)
    assertEquals(NavigationCameraMode.FOLLOW_USER_WITH_BEARING, state.cameraMode)
  }

  @Test
  fun routeOverviewSwitchesToOverviewMode() {
    val cameraState = mockCameraState(initialPosition = CameraPosition())
    val state = createState(cameraState = cameraState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.showRouteOverview(
        boundingBox = BoundingBox(north = 48.5, east = 16.7, south = 48.1, west = 16.2),
        paddingValues = PaddingValues(),
    )

    assertEquals(NavigationCameraMode.OVERVIEW, state.cameraMode)
    assertFalse(state.isTrackingUser)
    coVerify {
      cameraState.animateTo(
          boundingBox = any(),
          bearing = 0.0,
          tilt = 0.0,
          padding = PaddingValues(),
          duration = DEFAULT_ROUTE_OVERVIEW_ANIMATION_DURATION,
      )
    }
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

  private fun mockCameraState(
      initialPosition: CameraPosition,
      projection: CameraProjection? = null,
  ): CameraState {
    val cameraState = mockk<CameraState>(relaxed = true)
    var currentPosition = initialPosition

    every { cameraState.projection } returns projection
    every { cameraState.position } answers { currentPosition }
    every { cameraState.position = any() } answers { currentPosition = firstArg() }
    coEvery { cameraState.animateTo(any<CameraPosition>(), any<Duration>()) } answers {
      currentPosition = firstArg()
    }
    coEvery {
      cameraState.animateTo(
          any<org.maplibre.spatialk.geojson.BoundingBox>(),
          any<Double>(),
          any<Double>(),
          any<PaddingValues>(),
          any<Duration>(),
      )
    } returns Unit

    return cameraState
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
