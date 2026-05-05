package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpOffset
import com.stadiamaps.ferrostar.core.BoundingBox
import kotlin.math.ln
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.location.Location
import org.maplibre.compose.camera.rememberCameraState

internal val DEFAULT_TRACKING_CAMERA_TRANSITION_DURATION = 1000.milliseconds
internal val DEFAULT_ROUTE_OVERVIEW_ANIMATION_DURATION = 1000.milliseconds
internal val DEFAULT_ZOOM_ANIMATION_DURATION = 300.milliseconds

@Stable
class NavigationMapState
internal constructor(
    val cameraState: CameraState,
    cameraModeState: MutableState<NavigationCameraMode>,
    navigationCameraOptions: NavigationCameraOptions,
    private val coroutineScope: CoroutineScope,
) {
  private var cameraAnimationJob: Job? = null
  internal var suppressTrackingUpdates: Boolean = false

  var cameraMode by cameraModeState

  var navigationCameraOptions by mutableStateOf(navigationCameraOptions)

  val isTrackingUser: Boolean
    get() = cameraMode.tracksLocation()

  fun recenter(isNavigating: Boolean) {
    cameraMode = defaultNavigationCameraMode(isNavigating)
  }

  fun zoomIn(
      delta: Double = 1.0,
      duration: Duration = DEFAULT_ZOOM_ANIMATION_DURATION,
  ) {
    animateZoomBy(delta = delta, duration = duration)
  }

  fun zoomOut(
      delta: Double = 1.0,
      duration: Duration = DEFAULT_ZOOM_ANIMATION_DURATION,
  ) {
    animateZoomBy(delta = -delta, duration = duration)
  }

  fun panBy(screenDistance: DpOffset) {
    val projection = cameraState.projection ?: return
    val currentPosition = cameraState.position
    val translatedTarget =
        projection.positionFromScreenLocation(
            projection.screenLocationFromPosition(currentPosition.target) + screenDistance)

    cameraMode = NavigationCameraMode.FREE
    cameraState.position = currentPosition.copy(target = translatedTarget)
  }

  fun flingBy(screenDistance: DpOffset, duration: Duration = 300.milliseconds) {
    val projection = cameraState.projection ?: return
    val currentPosition = cameraState.position
    val translatedTarget =
        projection.positionFromScreenLocation(
            projection.screenLocationFromPosition(currentPosition.target) + screenDistance)

    cameraMode = NavigationCameraMode.FREE
    launchCameraAnimation {
      cameraState.animateTo(
          finalPosition = currentPosition.copy(target = translatedTarget),
          duration = duration,
      )
    }
  }

  fun scaleBy(scaleFactor: Float) {
    if (scaleFactor <= 0f) return

    val zoomDelta = ln(scaleFactor.toDouble()) / ln(2.0)
    cameraMode = NavigationCameraMode.FREE
    cameraState.position =
        cameraState.position.copy(zoom = (cameraState.position.zoom + zoomDelta).coerceAtLeast(0.0))
  }

  fun showRouteOverview(
      boundingBox: BoundingBox,
      paddingValues: PaddingValues = PaddingValues(),
      duration: Duration = DEFAULT_ROUTE_OVERVIEW_ANIMATION_DURATION,
  ) {
    cameraMode = NavigationCameraMode.OVERVIEW
    launchCameraAnimation {
      cameraState.animateTo(
          boundingBox = boundingBox.toMapLibreBoundingBox(),
          padding = paddingValues,
          duration = normalizeOverviewAnimationDuration(duration),
      )
    }
  }

  internal fun animateTrackingCameraToUserLocation(
      userLocation: Location,
      duration: Duration = DEFAULT_TRACKING_CAMERA_TRANSITION_DURATION,
  ) {
    launchCameraAnimation(suppressTrackingUpdates = true) {
      cameraState.animateTo(
          finalPosition =
              templateFollowingCameraPosition(
                  target = userLocation.position,
                  bearing = userLocation.bearing,
              ),
          duration = duration,
      )
    }
  }

  private fun animateZoomBy(
      delta: Double,
      duration: Duration,
  ) {
    if (delta == 0.0) return

    val targetZoom = (cameraState.position.zoom + delta).coerceAtLeast(0.0)
    if (isTrackingUser) {
      animateTrackingZoomTo(targetZoom = targetZoom, duration = duration)
    } else {
      launchCameraAnimation {
        cameraState.animateTo(
            finalPosition = cameraState.position.copy(zoom = targetZoom),
            duration = duration,
        )
      }
    }
  }

  private fun animateTrackingZoomTo(
      targetZoom: Double,
      duration: Duration,
  ) {
    launchCameraAnimation {
      val animatedZoom = Animatable(cameraState.position.zoom.toFloat())
      animatedZoom.animateTo(
          targetValue = targetZoom.toFloat(),
          animationSpec =
              tween(
                  durationMillis = duration.inWholeMilliseconds.toInt(),
                  easing = LinearEasing,
              ),
      ) {
        cameraState.position = cameraState.position.copy(zoom = value.coerceAtLeast(0f).toDouble())
      }
    }
  }

  private fun launchCameraAnimation(
      suppressTrackingUpdates: Boolean = false,
      block: suspend () -> Unit,
  ) {
    cameraAnimationJob?.cancel()
    this.suppressTrackingUpdates = suppressTrackingUpdates
    cameraAnimationJob =
        coroutineScope.launch {
          try {
            block()
          } finally {
            this@NavigationMapState.suppressTrackingUpdates = false
          }
        }
  }
}

// MapLibre Compose 0.12.1 crashes on Android when the bounds-animation path forwards a zero
// duration to MapLibreMap.animateCamera(...), which throws
// IllegalArgumentException("Null duration passed into animateCamera"). Keep overview transitions
// at >= 1 ms until that Android adapter path is fixed upstream.
internal fun normalizeOverviewAnimationDuration(duration: Duration): Duration =
    if (duration.inWholeMilliseconds <= 0L) {
      1.milliseconds
    } else {
      duration
    }

@Composable
fun rememberNavigationMapState(
    initialCameraMode: NavigationCameraMode = defaultNavigationCameraMode(isNavigating = false),
    navigationCameraOptions: NavigationCameraOptions = navigationCameraOptions(),
): NavigationMapState {
  val savedCameraMode = rememberSaveable { mutableStateOf(initialCameraMode) }
  val cameraState = rememberCameraState()
  val coroutineScope = rememberCoroutineScope()
  val navigationMapState =
      remember(cameraState, coroutineScope) {
        NavigationMapState(
            cameraState = cameraState,
            cameraModeState = savedCameraMode,
            navigationCameraOptions = navigationCameraOptions,
            coroutineScope = coroutineScope,
        )
      }

  navigationMapState.navigationCameraOptions = navigationCameraOptions
  return navigationMapState
}
