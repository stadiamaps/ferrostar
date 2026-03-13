package com.stadiamaps.ferrostar.carapp.navigation

import android.content.Context
import android.util.Log
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import com.stadiamaps.ferrostar.carapp.template.models.buildNavigationTrip
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.NavigationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uniffi.ferrostar.DrivingSide

/**
 * Bridges Ferrostar's [NavigationViewModel] to Car App Library's [NavigationManager].
 *
 * This component handles:
 * - Calling [NavigationManager.navigationStarted] / [NavigationManager.navigationEnded] at the
 *   correct lifecycle points (NF-4, NF-5).
 * - Feeding [NavigationManager.updateTrip] on each state update.
 * - Delegating [NavigationManagerCallback.onStopNavigation] to the provided callback.
 * - Delegating [NavigationManagerCallback.onAutoDriveEnabled] for NF-7 simulation support.
 *
 * Auto-drive simulation is intentionally NOT included — apps should implement [onAutoDriveEnabled]
 * themselves, as it requires direct access to [FerrostarCore] for location injection.
 *
 * @param navigationManager The Car App Library NavigationManager from CarContext.
 * @param viewModel The Ferrostar NavigationViewModel to observe.
 * @param context The context used to resolve maneuver icon drawables.
 * @param notificationManager Optional notification manager for HUN updates (NF-3).
 * @param drivingSide Driving side for maneuver mapping. Defaults to RIGHT.
 * @param onStopNavigation Called when the head unit requests navigation stop.
 * @param onAutoDriveEnabled Called when auto-drive simulation is requested (NF-7). Optional.
 */
class NavigationManagerBridge(
    private val navigationManager: NavigationManager,
    private val viewModel: NavigationViewModel,
    private val context: Context,
    private val notificationManager: TurnByTurnNotificationManager? = null,
    private val drivingSide: DrivingSide = DrivingSide.RIGHT,
    private val onStopNavigation: () -> Unit,
    private val onAutoDriveEnabled: (() -> Unit)? = null
) {

  private var observationJob: Job? = null
  private var wasNavigating = false

  /**
   * Starts observing the view model's navigation state and driving the [NavigationManager].
   *
   * Call this when the navigation session begins. The [scope] should be tied to the Car App Session
   * or Screen lifecycle.
   */
  fun start(scope: CoroutineScope) {
    navigationManager.setNavigationManagerCallback(
        object : NavigationManagerCallback {
          override fun onStopNavigation() {
            this@NavigationManagerBridge.onStopNavigation()
          }

          override fun onAutoDriveEnabled() {
            this@NavigationManagerBridge.onAutoDriveEnabled?.invoke()
          }
        })

    observationJob =
        viewModel.navigationUiState
            .onEach { state -> onNavigationStateUpdate(state) }
            .launchIn(scope)
  }

  /**
   * Stops observing navigation state and cleans up the [NavigationManager].
   *
   * Call this when the navigation session ends or the Car App Session is destroyed.
   */
  fun stop() {
    observationJob?.cancel()
    observationJob = null

    notificationManager?.clear()

    if (wasNavigating) {
      navigationManager.navigationEnded()
      wasNavigating = false
    }

    navigationManager.clearNavigationManagerCallback()
  }

  private fun onNavigationStateUpdate(uiState: NavigationUiState) {
    val isNavigating = uiState.isNavigating()

    if (isNavigating && !wasNavigating) {
      navigationManager.navigationStarted()
      wasNavigating = true
    }

    if (isNavigating) {
      updateTrip(uiState)
    }

    if (!isNavigating && wasNavigating) {
      notificationManager?.clear()
      navigationManager.navigationEnded()
      wasNavigating = false
    }
  }

  private fun updateTrip(uiState: NavigationUiState) {
    val instruction = uiState.visualInstruction ?: return
    val progress = uiState.progress

    val trip =
        buildNavigationTrip(
            instruction = instruction,
            progress = progress,
            context = context,
            drivingSide = drivingSide) ?: return

    try {
      navigationManager.updateTrip(trip)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to update trip", e)
    }

    notificationManager?.update(instruction)
  }

  companion object {
    private const val TAG = "NavManagerBridge"
  }
}
