package com.stadiamaps.ferrostar.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.extensions.currentRoadName
import com.stadiamaps.ferrostar.core.extensions.deviation
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.extensions.remainingSteps
import com.stadiamaps.ferrostar.core.extensions.visualInstruction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.RouteStep
import uniffi.ferrostar.SpokenInstruction
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.VisualInstruction

data class NavigationUiState(
    /** The user's location as reported by the location provider. */
    val location: UserLocation?,
    /** The user's location snapped to the route shape. */
    val snappedLocation: UserLocation?,
    /**
     * The last known heading of the user.
     *
     * NOTE: This is distinct from the course over ground (direction of travel), which is included
     * in the `location` and `snappedLocation` properties.
     */
    val heading: Float?,
    /** The geometry of the full route. */
    val routeGeometry: List<GeographicCoordinate>?,
    /** Visual instructions which should be displayed based on the user's current progress. */
    val visualInstruction: VisualInstruction?,
    /**
     * Instructions which should be spoken via speech synthesis based on the user's current
     * progress.
     */
    val spokenInstruction: SpokenInstruction?,
    /** The user's progress through the current trip. */
    val progress: TripProgress?,
    /** If true, the core is currently calculating a new route. */
    val isCalculatingNewRoute: Boolean?,
    /** Describes whether the user is believed to be off the correct route. */
    val routeDeviation: RouteDeviation?,
    /** If true, spoken instructions will not be synthesized. */
    val isMuted: Boolean?,
    /** The name of the road which the current route step is traversing. */
    val currentStepRoadName: String?,
    /** The remaining steps in the trip (including the current step). */
    val remainingSteps: List<RouteStep>?
) {
  companion object {
    fun fromFerrostar(
        coreState: NavigationState,
        isMuted: Boolean?,
        location: UserLocation?,
        snappedLocation: UserLocation?
    ): NavigationUiState =
        NavigationUiState(
            snappedLocation = snappedLocation,
            location = location,
            // TODO: Heading/course over ground
            heading = null,
            routeGeometry = coreState.routeGeometry,
            visualInstruction = coreState.tripState.visualInstruction(),
            spokenInstruction = null,
            progress = coreState.tripState.progress(),
            isCalculatingNewRoute = coreState.isCalculatingNewRoute,
            routeDeviation = coreState.tripState.deviation(),
            isMuted = isMuted,
            currentStepRoadName = coreState.tripState.currentRoadName(),
            remainingSteps = coreState.tripState.remainingSteps())
  }
}

interface NavigationViewModel {
  val uiState: StateFlow<NavigationUiState>

  fun toggleMute()

  fun stopNavigation()

  fun isNavigating(): Boolean = uiState.value.progress != null

  // TODO: We think the camera may eventually need to be owned by the view model, but that's going
  // to be a very big refactor (maybe even crossing into the MapLibre Compose project)
}

class DefaultNavigationViewModel(
    private val ferrostarCore: FerrostarCore,
    private val spokenInstructionObserver: SpokenInstructionObserver? = null,
    private val locationProvider: LocationProvider
) : ViewModel(), NavigationViewModel {

  private var userLocation: UserLocation? = locationProvider.lastLocation

  override val uiState =
      ferrostarCore.state
          .map { coreState ->
            val location = locationProvider.lastLocation
            userLocation =
                when (coreState.tripState) {
                  is TripState.Navigating -> coreState.tripState.snappedUserLocation
                  is TripState.Complete,
                  TripState.Idle -> locationProvider.lastLocation
                }
            uiState(coreState, spokenInstructionObserver?.isMuted, location, userLocation)
            // This awkward dance is required because Kotlin doesn't have a way to map over
            // StateFlows
            // without converting to a generic Flow in the process.
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              initialValue =
                  uiState(
                      ferrostarCore.state.value,
                      spokenInstructionObserver?.isMuted,
                      locationProvider.lastLocation,
                      userLocation))

  override fun stopNavigation() {
    ferrostarCore.stopNavigation()
  }

  override fun toggleMute() {
    if (spokenInstructionObserver == null) {
      Log.d("NavigationViewModel", "Spoken instruction observer is null, mute operation ignored.")
      return
    }
    spokenInstructionObserver.isMuted = !spokenInstructionObserver.isMuted
  }

  // TODO: We can add a hook here to override the current road name.
  // Eventually someone will probably want local map matching, vector tile inspection.
  private fun uiState(
      coreState: NavigationState,
      isMuted: Boolean?,
      location: UserLocation?,
      snappedLocation: UserLocation?
  ) = NavigationUiState.fromFerrostar(coreState, isMuted, location, snappedLocation)
}
