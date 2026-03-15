package com.stadiamaps.ferrostar.car.app.navigation

import android.content.Context
import android.util.Log
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Destination
import androidx.lifecycle.Lifecycle
import com.stadiamaps.ferrostar.car.app.template.models.FerrostarTrip
import com.stadiamaps.ferrostar.core.NavigationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
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
 * @param context The context used to resolve maneuver icon drawables.
 * @param notificationManager Optional notification manager for HUN updates (NF-3).
 * @param viewModel The Ferrostar NavigationViewModel to observe.
 * @param backupDrivingSide Driving side for maneuver mapping. Defaults to RIGHT.
 * @param onStopNavigation Called when the head unit requests navigation stop.
 * @param onAutoDriveEnabled Called when auto-drive simulation is requested (NF-7). Optional.
 * @param isCarForeground Returns true when the car app screen is visible to the user. When true,
 *   turn-by-turn notifications are suppressed since the screen itself shows the guidance. Pass
 *   `{ lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }` from your [Screen].
 */
class NavigationManagerBridge(
    private val navigationManager: NavigationManager,
    private val context: Context,
    private val notificationManager: TurnByTurnNotificationManager? = null,
    private val viewModel: NavigationViewModel,
    private val backupDrivingSide: DrivingSide = DrivingSide.RIGHT,
    private val onStopNavigation: () -> Unit,
    private val onAutoDriveEnabled: (() -> Unit)? = null,
    private val isCarForeground: () -> Boolean = { false }
) {

  private var tripJob: Job? = null
  private var notificationJob: Job? = null
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

    // Trip lifecycle and updateTrip on every state change.
    tripJob =
        viewModel.navigationUiState
            .onEach { state ->
              val isNavigating = state.isNavigating()

              if (isNavigating && !wasNavigating) {
                navigationManager.navigationStarted()
                wasNavigating = true
              }

              if (isNavigating) {
                state.tripState?.let {
                  val trip =
                      FerrostarTrip.Builder(context)
                          .setTripState(it)
                          .setBackupDrivingSide(backupDrivingSide)
                          .apply { state.destination?.let { dest -> setDestination(dest) } }
                          .build()
                  try {
                    navigationManager.updateTrip(trip)
                  } catch (e: Exception) {
                    Log.w(TAG, "Failed to update trip", e)
                  }
                }
              }

              if (!isNavigating && wasNavigating) {
                notificationManager?.clear()
                navigationManager.navigationEnded()
                wasNavigating = false
              }
            }
            .launchIn(scope)

    // Notification flow: emits once per instruction trigger zone entry.
    notificationJob =
        viewModel.navigationUiState
            .mapNotNull { state ->
              state.spokenInstruction
            }
            .distinctUntilChanged { old, new ->
              // Only emit if the instruction text has changed.
              // This is the core scheduling logic that emits a new instruction
              // only when the text has actually changed (trigger distance has arrived)
              old.text == new.text
            }
            .onEach { instruction ->
              if (!isCarForeground()) {
                notificationManager?.update(instruction)
              }
            }
            .launchIn(scope)
  }

  /**
   * Stops observing navigation state and cleans up the [NavigationManager].
   *
   * Call this when the navigation session ends or the Car App Session is destroyed.
   */
  fun stop() {
    tripJob?.cancel()
    tripJob = null
    notificationJob?.cancel()
    notificationJob = null

    notificationManager?.clear()

    if (wasNavigating) {
      navigationManager.navigationEnded()
      wasNavigating = false
    }

    navigationManager.clearNavigationManagerCallback()
  }

  companion object {
    private const val TAG = "NavManagerBridge"
  }
}
