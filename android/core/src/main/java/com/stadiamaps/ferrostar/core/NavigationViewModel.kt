package com.stadiamaps.ferrostar.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.extensions.deviation
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.extensions.visualInstruction
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Heading
import uniffi.ferrostar.RouteDeviation
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
    val isMuted: Boolean?
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
            isMuted = isMuted)
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

class IdleNavigationViewModel(locationProvider: LocationProvider) :
    ViewModel(), NavigationViewModel {
  private val locationStateFlow = MutableStateFlow(locationProvider.lastLocation)
  private val executor = Executors.newSingleThreadScheduledExecutor()

  init {
    locationProvider.addListener(
        object : LocationUpdateListener {
          override fun onLocationUpdated(location: UserLocation) {
            locationStateFlow.update { location }
          }

          override fun onHeadingUpdated(heading: Heading) {
            // TODO: Heading
          }
        },
        executor)
  }

  override val uiState =
      locationStateFlow
          .map { userLocation ->
            // TODO: Heading
            NavigationUiState(userLocation, null, null, null, null, null, null, false, null, null)
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              // TODO: Heading
              initialValue =
                  NavigationUiState(
                      locationProvider.lastLocation,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      false,
                      null,
                      null))

  override fun toggleMute() {
    // Do nothing
  }

  override fun stopNavigation() {
    // Do nothing
  }
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

  private fun uiState(
      coreState: NavigationState,
      isMuted: Boolean?,
      location: UserLocation?,
      snappedLocation: UserLocation?
  ) = NavigationUiState.fromFerrostar(coreState, isMuted, location, snappedLocation)
}
