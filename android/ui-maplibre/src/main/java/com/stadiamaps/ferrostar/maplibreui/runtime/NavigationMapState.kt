package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import com.stadiamaps.ferrostar.core.BoundingBox
import kotlin.math.ln
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.location.LocationMeasurement
import org.maplibre.compose.map.MapState
import org.maplibre.compose.util.toDpPadding

internal val DEFAULT_TRACKING_CAMERA_TRANSITION_DURATION = 1000.milliseconds
internal val DEFAULT_ROUTE_OVERVIEW_ANIMATION_DURATION = 1000.milliseconds
internal val DEFAULT_ZOOM_ANIMATION_DURATION = 300.milliseconds

/**
 * Ferrostar camera state for a navigation map.
 *
 * Create it with [rememberNavigationMapState] and pass it to a navigation map view. The view
 * attaches its [MapState], and camera commands do nothing while no view displays this state.
 */
@Stable
class NavigationMapState
internal constructor(
    cameraModeState: MutableState<NavigationCameraMode>,
    retainedCameraPositionState: MutableState<CameraPosition>,
    navigationCameraOptions: NavigationCameraOptions,
    private val coroutineScope: CoroutineScope,
) {
  private var cameraAnimationJob: Job? = null
  internal var suppressTrackingUpdates: Boolean = false
  internal var layoutDirection: LayoutDirection = LayoutDirection.Ltr
  private var retainedCameraPosition by retainedCameraPositionState

  var cameraMode by cameraModeState

  var navigationCameraOptions by mutableStateOf(navigationCameraOptions)

  /**
   * The MapLibre map state of the navigation map view that displays this state, or null while no
   * view displays it.
   *
   * Use it for projection, feature queries, and direct camera commands. Set [cameraMode] to
   * [NavigationCameraMode.FREE] before you move the camera, or the user tracking replaces your
   * camera.
   */
  var mapState: MapState? by mutableStateOf(null)
    private set

  /**
   * The current camera. While no view displays this state, it is the last camera of the most recent
   * view.
   */
  val cameraPosition: CameraPosition
    get() = mapState?.cameraPosition ?: retainedCameraPosition

  val isTrackingUser: Boolean
    get() = cameraMode.tracksLocation()

  internal fun attach(mapState: MapState) {
    this.mapState = mapState
  }

  internal fun detach(mapState: MapState) {
    if (this.mapState !== mapState) return
    retainedCameraPosition = mapState.cameraPosition
    cameraAnimationJob?.cancel()
    this.mapState = null
  }

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
    val mapState = mapState ?: return
    val currentPosition = mapState.cameraPosition
    val translatedCenter = mapState.translate(currentPosition, screenDistance) ?: return

    cameraMode = NavigationCameraMode.FREE
    mapState.setCameraPosition(currentPosition.copy(center = translatedCenter))
  }

  fun flingBy(screenDistance: DpOffset, duration: Duration = 300.milliseconds) {
    val mapState = mapState ?: return
    val currentPosition = mapState.cameraPosition
    val translatedCenter = mapState.translate(currentPosition, screenDistance) ?: return

    cameraMode = NavigationCameraMode.FREE
    launchCameraAnimation {
      mapState.animateCamera(
          update = currentPosition.copy(center = translatedCenter).toCameraUpdate(),
          animation = flight(duration),
      )
    }
  }

  fun scaleBy(scaleFactor: Float) {
    if (scaleFactor <= 0f) return
    val mapState = mapState ?: return

    val zoomDelta = ln(scaleFactor.toDouble()) / ln(2.0)
    cameraMode = NavigationCameraMode.FREE
    mapState.incrementZoom(zoomDelta)
  }

  /**
   * Switches to [NavigationCameraMode.OVERVIEW] and animates the camera to fit [boundingBox].
   *
   * [paddingValues] becomes the padding of the resulting camera, and the bounds fit inside it.
   */
  fun showRouteOverview(
      boundingBox: BoundingBox,
      paddingValues: PaddingValues = PaddingValues(),
      duration: Duration = DEFAULT_ROUTE_OVERVIEW_ANIMATION_DURATION,
  ) {
    cameraMode = NavigationCameraMode.OVERVIEW
    val mapState = mapState ?: return
    val fit = CameraFit(cameraPadding = paddingValues.toDpPadding(layoutDirection))
    launchCameraAnimation {
      mapState.animateCameraToBounds(
          boundingBox = boundingBox.toMapLibreBoundingBox(),
          fit = fit,
          animation = flight(duration),
      )
    }
  }

  internal fun animateTrackingCameraToUserLocation(
      userLocation: LocationMeasurement,
      duration: Duration = DEFAULT_TRACKING_CAMERA_TRANSITION_DURATION,
  ) {
    val mapState = mapState ?: return
    val finalPosition =
        templateFollowingCameraPosition(
            target = userLocation.position,
            bearing = userLocation.courseDegrees,
        )
    launchCameraAnimation(suppressTrackingUpdates = true) {
      mapState.animateCamera(
          update = finalPosition.toCameraUpdate(),
          animation = flight(duration),
      )
    }
  }

  private fun animateZoomBy(
      delta: Double,
      duration: Duration,
  ) {
    if (delta == 0.0) return
    val mapState = mapState ?: return

    val targetZoom = (mapState.cameraPosition.zoom + delta).coerceAtLeast(0.0)
    if (isTrackingUser) {
      animateTrackingZoomTo(mapState = mapState, targetZoom = targetZoom, duration = duration)
    } else {
      val finalPosition = mapState.cameraPosition.copy(zoom = targetZoom)
      launchCameraAnimation {
        mapState.animateCamera(
            update = finalPosition.toCameraUpdate(),
            animation = flight(duration),
        )
      }
    }
  }

  private fun animateTrackingZoomTo(
      mapState: MapState,
      targetZoom: Double,
      duration: Duration,
  ) {
    launchCameraAnimation {
      val animatedZoom = Animatable(mapState.cameraPosition.zoom.toFloat())
      animatedZoom.animateTo(
          targetValue = targetZoom.toFloat(),
          animationSpec =
              tween(
                  durationMillis = duration.inWholeMilliseconds.toInt(),
                  easing = LinearEasing,
              ),
      ) {
        mapState.setCameraPosition(
            mapState.cameraPosition.copy(zoom = value.coerceAtLeast(0f).toDouble())
        )
      }
    }
  }

  private fun launchCameraAnimation(
      suppressTrackingUpdates: Boolean = false,
      block: suspend () -> Unit,
  ) {
    cameraAnimationJob?.cancel()
    this.suppressTrackingUpdates = suppressTrackingUpdates
    cameraAnimationJob = coroutineScope.launch {
      try {
        block()
      } finally {
        this@NavigationMapState.suppressTrackingUpdates = false
      }
    }
  }
}

private fun MapState.translate(position: CameraPosition, screenDistance: DpOffset) =
    screenLocationFromPosition(position.center)?.let {
      positionFromScreenLocation(it + screenDistance)
    }

private fun flight(duration: Duration): CameraAnimation.Fly = CameraAnimation.Fly {
  this.duration = duration.coerceAtLeast(Duration.ZERO)
}

private val CameraPositionSaver =
    Saver<CameraPosition, String>(
        save = { Json.encodeToString(CameraPosition.serializer(), it) },
        restore = { Json.decodeFromString(CameraPosition.serializer(), it) },
    )

@Composable
fun rememberNavigationMapState(
    initialCameraMode: NavigationCameraMode = defaultNavigationCameraMode(isNavigating = false),
    navigationCameraOptions: NavigationCameraOptions = navigationCameraOptions(),
): NavigationMapState {
  val savedCameraMode = rememberSaveable { mutableStateOf(initialCameraMode) }
  val retainedCameraPosition =
      rememberSaveable(stateSaver = CameraPositionSaver) { mutableStateOf(CameraPosition()) }
  val coroutineScope = rememberCoroutineScope()
  val navigationMapState =
      remember(coroutineScope) {
        NavigationMapState(
            cameraModeState = savedCameraMode,
            retainedCameraPositionState = retainedCameraPosition,
            navigationCameraOptions = navigationCameraOptions,
            coroutineScope = coroutineScope,
        )
      }

  navigationMapState.navigationCameraOptions = navigationCameraOptions
  navigationMapState.layoutDirection = LocalLayoutDirection.current
  return navigationMapState
}
