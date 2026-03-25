package com.stadiamaps.ferrostar.maplibreui.runtime

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.stadiamaps.ferrostar.core.BoundingBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState

@Stable
class NavigationMapState
internal constructor(
    internal val cameraState: CameraState,
    initialCameraMode: NavigationCameraMode,
    navigationCameraOptions: NavigationCameraOptions,
    private val coroutineScope: CoroutineScope,
) {
  private var routeOverviewJob: Job? = null

  var cameraMode by mutableStateOf(initialCameraMode)

  var navigationCameraOptions by mutableStateOf(navigationCameraOptions)

  val isTrackingUser: Boolean
    get() = cameraMode.tracksLocation()

  fun recenter(isNavigating: Boolean) {
    cameraMode = defaultNavigationCameraMode(isNavigating)
  }

  fun zoomIn(delta: Double = 1.0) {
    cameraState.incrementZoom(delta)
  }

  fun zoomOut(delta: Double = 1.0) {
    cameraState.incrementZoom(-delta)
  }

  fun showRouteOverview(
      boundingBox: BoundingBox,
      paddingValues: PaddingValues = PaddingValues(),
      duration: Duration = 0.milliseconds,
  ) {
    cameraMode = NavigationCameraMode.OVERVIEW
    routeOverviewJob?.cancel()
    routeOverviewJob =
        coroutineScope.launch {
          cameraState.animateTo(
              boundingBox = boundingBox.toMapLibreBoundingBox(),
              padding = paddingValues,
              duration = duration,
          )
        }
  }
}

@Composable
fun rememberNavigationMapState(
    initialCameraMode: NavigationCameraMode = defaultNavigationCameraMode(isNavigating = false),
    navigationCameraOptions: NavigationCameraOptions = navigationCameraOptions(),
): NavigationMapState {
  val cameraState = rememberCameraState()
  val coroutineScope = rememberCoroutineScope()
  val navigationMapState =
      remember(cameraState, coroutineScope) {
        NavigationMapState(
            cameraState = cameraState,
            initialCameraMode = initialCameraMode,
            navigationCameraOptions = navigationCameraOptions,
            coroutineScope = coroutineScope,
        )
      }

  navigationMapState.navigationCameraOptions = navigationCameraOptions
  return navigationMapState
}
