package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stadiamaps.ferrostar.core.BoundingBox
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.map.MapState
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox as MapLibreBoundingBox
import org.maplibre.spatialk.geojson.Position

class NavigationMapStateTest {
  /** Runs each frame at once, 16 ms after the previous one, so animations finish synchronously. */
  private val steppingFrameClock =
      object : MonotonicFrameClock {
        private var frameTimeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
          frameTimeNanos += 16_000_000L
          return onFrame(frameTimeNanos)
        }
      }
  private val testScope = CoroutineScope(Job() + Dispatchers.Unconfined + steppingFrameClock)

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
  fun zoomHelpersFlyCameraZoom() {
    val mapState = mockMapState(initialPosition = CameraPosition(zoom = 10.0))
    val state = createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FREE)

    state.zoomIn()
    state.zoomOut(delta = 2.0)

    coVerify {
      mapState.animateCamera(
          update = match { it.zoom == 11.0 },
          animation = match { it.isFlight(DEFAULT_ZOOM_ANIMATION_DURATION) },
      )
    }
    coVerify {
      mapState.animateCamera(
          update = match { it.zoom == 9.0 },
          animation = match { it.isFlight(DEFAULT_ZOOM_ANIMATION_DURATION) },
      )
    }
  }

  @Test
  fun zoomByZeroDoesNothing() {
    val mapState = mockMapState(initialPosition = CameraPosition(zoom = 10.0))
    val state = createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FREE)

    state.zoomIn(delta = 0.0)

    coVerify(exactly = 0) { mapState.animateCamera(any(), any()) }
  }

  @Test
  fun zoomWhileTrackingSetsZoomWithoutReplacingTheCenter() {
    val center = Position(16.0, 48.0)
    val mapState = mockMapState(initialPosition = CameraPosition(center = center, zoom = 10.0))
    val state =
        createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.zoomIn()

    coVerify(exactly = 0) { mapState.animateCamera(any(), any()) }
    verify { mapState.setCameraPosition(match { it.zoom == 11.0 && it.center == center }) }
    assertEquals(11.0, mapState.cameraPosition.zoom, 0.0)
  }

  @Test
  fun panBySwitchesToFreeCameraAndUpdatesCenter() {
    val initialPosition = CameraPosition(center = Position(16.0, 48.0), zoom = 10.0)
    val updatedCenter = Position(16.1, 48.2)
    val mapState = mockMapState(initialPosition = initialPosition)
    val state =
        createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    every { mapState.screenLocationFromPosition(initialPosition.center) } returns
        DpOffset(100.dp, 200.dp)
    every { mapState.positionFromScreenLocation(DpOffset(120.dp, 180.dp)) } returns updatedCenter

    state.panBy(DpOffset(20.dp, (-20).dp))

    assertEquals(NavigationCameraMode.FREE, state.cameraMode)
    assertEquals(updatedCenter, mapState.cameraPosition.center)
    assertEquals(initialPosition.zoom, mapState.cameraPosition.zoom, 0.0)
  }

  @Test
  fun panByIsNoOpWithoutProjection() {
    val initialPosition = CameraPosition(center = Position(16.0, 48.0), zoom = 10.0)
    val mapState = mockMapState(initialPosition = initialPosition)
    every { mapState.screenLocationFromPosition(any()) } returns null
    val state =
        createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.panBy(DpOffset(5.dp, 7.dp))

    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
    assertEquals(initialPosition, mapState.cameraPosition)
  }

  @Test
  fun flingBySwitchesToFreeCameraAndFliesToCenter() {
    val initialPosition = CameraPosition(center = Position(16.0, 48.0), zoom = 10.0)
    val updatedCenter = Position(16.3, 48.4)
    val mapState = mockMapState(initialPosition = initialPosition)
    val state =
        createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    every { mapState.screenLocationFromPosition(initialPosition.center) } returns
        DpOffset(50.dp, 80.dp)
    every { mapState.positionFromScreenLocation(DpOffset(65.dp, 100.dp)) } returns updatedCenter

    state.flingBy(DpOffset(15.dp, 20.dp), duration = 120.milliseconds)

    assertEquals(NavigationCameraMode.FREE, state.cameraMode)
    coVerify {
      mapState.animateCamera(
          update = match { it.center == updatedCenter },
          animation = match { it.isFlight(120.milliseconds) },
      )
    }
  }

  @Test
  fun flingByIsNoOpWithoutProjection() {
    val initialPosition = CameraPosition(center = Position(16.0, 48.0), zoom = 10.0)
    val mapState = mockMapState(initialPosition = initialPosition)
    every { mapState.screenLocationFromPosition(any()) } returns DpOffset(50.dp, 80.dp)
    every { mapState.positionFromScreenLocation(any()) } returns null
    val state =
        createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.flingBy(DpOffset(15.dp, 20.dp), duration = 120.milliseconds)

    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
    coVerify(exactly = 0) { mapState.animateCamera(any(), any()) }
  }

  @Test
  fun scaleByIgnoresNonPositiveFactors() {
    val mapState = mockMapState(CameraPosition(zoom = 10.0))
    val state =
        createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.scaleBy(0f)
    state.scaleBy(-1f)

    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
    assertEquals(10.0, mapState.cameraPosition.zoom, 0.0)
  }

  @Test
  fun scaleByUsesLogBase2ZoomDelta() {
    val mapState = mockMapState(CameraPosition(zoom = 10.0))
    val state =
        createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.scaleBy(2f)
    assertEquals(NavigationCameraMode.FREE, state.cameraMode)
    assertEquals(11.0, mapState.cameraPosition.zoom, 0.0)

    state.scaleBy(0.5f)
    assertEquals(10.0, mapState.cameraPosition.zoom, 0.0001)
  }

  @Test
  fun scaleByClampsZoomAtZero() {
    val mapState = mockMapState(CameraPosition(zoom = 0.25))
    val state = createState(mapState = mapState)

    state.scaleBy(0.1f)

    assertEquals(0.0, mapState.cameraPosition.zoom, 0.0)
  }

  @Test
  fun recenterUsesBrowsingTemplateZoomAfterFreeCamera() {
    val state =
        createState(
            mapState = mockMapState(CameraPosition(zoom = 10.0)),
            initialCameraMode = NavigationCameraMode.FREE,
        )

    state.recenter(isNavigating = false)

    val position =
        state.templateFollowingCameraPosition(
            target = CameraPosition().center,
            bearing = null,
        )

    assertEquals(16.0, position.zoom, 0.0)
    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)
  }

  @Test
  fun recenterUsesNavigationTemplateZoomAfterOverview() {
    val state =
        createState(
            mapState = mockMapState(CameraPosition(zoom = 10.0)),
            initialCameraMode = NavigationCameraMode.OVERVIEW,
        )

    state.recenter(isNavigating = true)

    val position =
        state.templateFollowingCameraPosition(
            target = CameraPosition().center,
            bearing = 87.0,
        )

    assertEquals(16.0, position.zoom, 0.0)
    assertEquals(87.0, position.bearing, 0.0)
    assertEquals(NavigationCameraMode.FOLLOW_USER_WITH_BEARING, state.cameraMode)
  }

  @Test
  fun routeOverviewSwitchesToOverviewModeAndFitsBoundsInsideThePadding() {
    val mapState = mockMapState(initialPosition = CameraPosition())
    val state =
        createState(mapState = mapState, initialCameraMode = NavigationCameraMode.FOLLOW_USER)
    state.layoutDirection = LayoutDirection.Rtl

    state.showRouteOverview(
        boundingBox = BoundingBox(north = 48.5, east = 16.7, south = 48.1, west = 16.2),
        paddingValues = PaddingValues(start = 10.dp, top = 20.dp, end = 30.dp, bottom = 40.dp),
    )

    assertEquals(NavigationCameraMode.OVERVIEW, state.cameraMode)
    assertFalse(state.isTrackingUser)
    coVerify {
      mapState.animateCameraToBounds(
          boundingBox = MapLibreBoundingBox(west = 16.2, south = 48.1, east = 16.7, north = 48.5),
          fit =
              CameraFit(
                  bearing = 0.0,
                  pitch = 0.0,
                  cameraPadding =
                      DpPadding(left = 30.dp, top = 20.dp, right = 10.dp, bottom = 40.dp),
              ),
          animation = match { it.isFlight(DEFAULT_ROUTE_OVERVIEW_ANIMATION_DURATION) },
      )
    }
  }

  @Test
  fun routeOverviewClampsNegativeDurationToZero() {
    val mapState = mockMapState(initialPosition = CameraPosition())
    val state = createState(mapState = mapState)

    state.showRouteOverview(
        boundingBox = BoundingBox(north = 48.5, east = 16.7, south = 48.1, west = 16.2),
        duration = (-50).milliseconds,
    )

    coVerify { mapState.animateCameraToBounds(any(), any(), match { it.isFlight(Duration.ZERO) }) }
  }

  @Test
  fun commandsWithoutAMapOnlyChangeTheCameraMode() {
    val state = createState(mapState = null, initialCameraMode = NavigationCameraMode.FOLLOW_USER)

    state.zoomIn()
    state.panBy(DpOffset(5.dp, 7.dp))
    state.flingBy(DpOffset(5.dp, 7.dp))
    state.scaleBy(2f)

    assertEquals(NavigationCameraMode.FOLLOW_USER, state.cameraMode)

    state.showRouteOverview(BoundingBox(north = 48.5, east = 16.7, south = 48.1, west = 16.2))

    assertEquals(NavigationCameraMode.OVERVIEW, state.cameraMode)
  }

  @Test
  fun cameraPositionReadsTheAttachedMap() {
    val attachedPosition = CameraPosition(center = Position(16.0, 48.0), zoom = 12.0)
    val state = createState(mapState = mockMapState(attachedPosition))

    assertEquals(attachedPosition, state.cameraPosition)
  }

  @Test
  fun detachRetainsTheLastCameraPosition() {
    val lastPosition = CameraPosition(center = Position(16.0, 48.0), zoom = 12.0, bearing = 30.0)
    val mapState = mockMapState(lastPosition)
    val state = createState(mapState = mapState)

    state.detach(mapState)

    assertNull(state.mapState)
    assertEquals(lastPosition, state.cameraPosition)
  }

  @Test
  fun detachIgnoresAMapThatIsNotAttached() {
    val attached = mockMapState(CameraPosition(zoom = 12.0))
    val replaced = mockMapState(CameraPosition(zoom = 3.0))
    val state = createState(mapState = attached)

    state.detach(replaced)

    assertSame(attached, state.mapState)
    assertEquals(12.0, state.cameraPosition.zoom, 0.0)
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

  private fun CameraAnimation.isFlight(duration: Duration): Boolean =
      this is CameraAnimation.Fly && this.duration == duration

  private fun mockMapState(initialPosition: CameraPosition): MapState {
    val mapState = mockk<MapState>(relaxed = true)
    var currentPosition = initialPosition

    every { mapState.cameraPosition } answers { currentPosition }
    every { mapState.setCameraPosition(any()) } answers { currentPosition = firstArg() }
    coEvery { mapState.animateCamera(any(), any()) } answers
        {
          currentPosition = currentPosition.updatedBy(firstArg())
        }

    return mapState
  }

  private fun CameraPosition.updatedBy(update: CameraUpdate): CameraPosition =
      copy(
          center = update.center ?: center,
          zoom = update.zoom ?: zoom,
          bearing = update.bearing ?: bearing,
          pitch = update.pitch ?: pitch,
          padding = update.padding ?: padding,
      )

  private fun createState(
      mapState: MapState? = mockMapState(CameraPosition()),
      initialCameraMode: NavigationCameraMode = defaultNavigationCameraMode(isNavigating = false),
  ): NavigationMapState =
      NavigationMapState(
              cameraModeState = mutableStateOf(initialCameraMode),
              retainedCameraPositionState = mutableStateOf(CameraPosition()),
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
          .also { state -> mapState?.let(state::attach) }
}
