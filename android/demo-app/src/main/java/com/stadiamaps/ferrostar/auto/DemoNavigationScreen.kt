package com.stadiamaps.ferrostar.auto

import android.graphics.Rect
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.Template
import androidx.car.app.navigation.NavigationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.DefaultLifecycleObserver

import androidx.lifecycle.LifecycleOwner
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.camera.extensions.incrementZoom
import com.maplibre.compose.camera.extensions.setZoom
import com.maplibre.compose.camera.models.CameraPadding
import com.maplibre.compose.car.ComposableScreen
import com.maplibre.compose.surface.SurfaceGestureCallback
import com.stadiamaps.ferrostar.AppModule
import com.stadiamaps.ferrostar.R
import com.stadiamaps.ferrostar.carapp.maplibreui.runtime.surfaceStableCameraPadding
import com.stadiamaps.ferrostar.carapp.maplibreui.runtime.surfaceStableFractionalCameraPadding
import com.stadiamaps.ferrostar.carapp.navigation.NavigationManagerBridge
import com.stadiamaps.ferrostar.carapp.navigation.TurnByTurnNotificationManager
import com.stadiamaps.ferrostar.carapp.template.NavigationTemplateBuilder
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.boundingBox
import com.stadiamaps.ferrostar.maplibreui.runtime.navigationMapViewCamera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uniffi.ferrostar.DrivingSide

/**
 * A basic Android Auto navigation screen that demonstrates how to use the Ferrostar car-app library
 * components with a MapLibre map surface.
 *
 * This screen:
 * - Renders a [CarAppNavigationView] on the car display surface via [ComposableScreen]
 * - Observes the navigation view model for navigation state
 * - Builds a [NavigationTemplate] with routing info (maneuver + distance) when navigating
 * - Wires up [NavigationManagerBridge] for NF-4/NF-5 compliance
 * - Posts turn-by-turn notifications via [TurnByTurnNotificationManager] for NF-3 compliance
 */
class DemoNavigationScreen(carContext: CarContext) : ComposableScreen(carContext) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val viewModel = AppModule.viewModel
  private var observeJob: Job? = null

  private val notificationManager =
      TurnByTurnNotificationManager(context = carContext, smallIconRes = R.drawable.ic_navigation)

  private val navigationManagerBridge =
      NavigationManagerBridge(
          navigationManager = carContext.getCarService(NavigationManager::class.java),
          viewModel = viewModel,
          context = carContext,
          notificationManager = notificationManager,
          drivingSide = DrivingSide.RIGHT,
          onStopNavigation = { viewModel.stopNavigation() })

  private var uiState: NavigationUiState? by mutableStateOf(null)

  private var stableArea: Rect? by mutableStateOf(null)
  private var visibleArea: Rect? by mutableStateOf(null)
  private val containerArea: Rect?
    get() {
      val stable = stableArea ?: return null
      val visible = visibleArea ?: return null
      return Rect(stable.left, visible.top, stable.right, visible.bottom)
    }

  init {
    surfaceGestureCallback = object : SurfaceGestureCallback {
      override fun onStableAreaChanged(stableArea: Rect) {
        this@DemoNavigationScreen.stableArea = stableArea
      }

      override fun onVisibleAreaChanged(visibleArea: Rect) {
        this@DemoNavigationScreen.visibleArea = visibleArea
      }
    }

    navigationManagerBridge.start(scope)

    observeJob =
        viewModel.navigationUiState
            .onEach { state ->
              uiState = state
              invalidate()
            }
            .launchIn(scope)
}

  @Composable
  override fun content() {
      val normalPaddingState = rememberUpdatedState(surfaceStableFractionalCameraPadding(stableArea))
      val trackingPaddingState = rememberUpdatedState(surfaceStableFractionalCameraPadding(stableArea, top = 0.5f))
      val camera = remember { mutableStateOf(viewModel.mapViewCamera.value) }

      // Transition to navigation camera when navigation starts
      LaunchedEffect(uiState?.isNavigating()) {
          if (uiState?.isNavigating() == true) {
              viewModel.mapViewCamera.value = viewModel.navigationCamera.value
          }
      }

      // Sync camera with padding based on current state
      LaunchedEffect(Unit) {
          snapshotFlow {
              val base = viewModel.mapViewCamera.value
              val normalPadding = normalPaddingState.value
              val trackingPadding = trackingPaddingState.value
              if (uiState?.isNavigating() == true) {
                  when (base.state) {
                      is CameraState.TrackingUserLocation,
                      is CameraState.TrackingUserLocationWithBearing -> base.copy(padding = trackingPadding)
                      else -> base.copy(padding = normalPadding)
                  }
              } else {
                  base
              }
          }.collect { camera.value = it }
      }

      DemoNavigationView(
          viewModel,
          camera = camera,
          stableArea = containerArea
      )
  }

  override fun onGetTemplate(): Template {
    uiState?.let { state ->
        if (state.isNavigating()) {
            return NavigationTemplateBuilder(carContext)
                .setDrivingSite(DrivingSide.RIGHT)
                .setOnStopNavigation {
                    viewModel.stopNavigation()
                }
                .setOnMute(uiState?.isMuted) {
                    viewModel.toggleMute()
                }
                .setOnZoom(
                    onZoomInTapped = { viewModel.zoomIn() },
                    onZoomOutTapped = { viewModel.zoomOut() }
                )
                .setOnCycleCamera(viewModel.isTrackingUser()) {
                  viewModel.centerCamera()
                }
                .setVisualInstruction(state.visualInstruction)
                .setTripProgress(state.progress)
                .build()
        }
    }

    // Fall back to a basic map template of your App's preference here.
    return buildDemoMapTemplate()
  }

  init {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
          override fun onDestroy(owner: LifecycleOwner) {
            navigationManagerBridge.stop()
            observeJob?.cancel()
            scope.cancel()
          }
        })
  }

    companion object {
        private const val TAG = "DemoNavigationScreen"
    }
}
