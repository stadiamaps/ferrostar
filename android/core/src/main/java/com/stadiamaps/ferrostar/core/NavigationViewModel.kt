package com.stadiamaps.ferrostar.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.extensions.annotation
import com.stadiamaps.ferrostar.core.extensions.deviation
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.extensions.visualInstruction
import com.stadiamaps.ferrostar.core.models.AnnotationValue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.SpokenInstruction
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.VisualInstruction

data class NavigationUiState(
    val snappedLocation: UserLocation?,
    val heading: Float?,
    val routeGeometry: List<GeographicCoordinate>,
    val visualInstruction: VisualInstruction?,
    val spokenInstruction: SpokenInstruction?,
    val progress: TripProgress?,
    val isCalculatingNewRoute: Boolean?,
    val routeDeviation: RouteDeviation?,
    val isMuted: Boolean?
) {
  companion object {
    fun fromFerrostar(
        coreState: NavigationState,
        isMuted: Boolean?,
        userLocation: UserLocation?
    ): NavigationUiState =
        NavigationUiState(
            snappedLocation = userLocation,
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
}

class DefaultNavigationViewModel(
    private val ferrostarCore: FerrostarCore,
    private val spokenInstructionObserver: SpokenInstructionObserver? = null,
    locationProvider: LocationProvider
) : ViewModel(), NavigationViewModel {

  private var lastLocation: UserLocation? = locationProvider.lastLocation

  override val uiState =
      ferrostarCore.state
          .map { coreState ->
            lastLocation =
                when (coreState.tripState) {
                  is TripState.Navigating -> {
                    Log.d("NavigationViewModel", "current annotations: ${coreState.tripState.annotation(
                      AnnotationValue::class.java)}")

                    coreState.tripState.snappedUserLocation
                  }
                  is TripState.Complete,
                  TripState.Idle -> locationProvider.lastLocation
                }

            uiState(coreState, spokenInstructionObserver?.isMuted, lastLocation)
            // This awkward dance is required because Kotlin doesn't have a way to map over
            // StateFlows
            // without converting to a generic Flow in the process.
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              initialValue =
                  uiState(
                      ferrostarCore.state.value, spokenInstructionObserver?.isMuted, lastLocation))

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

  private fun uiState(coreState: NavigationState, isMuted: Boolean?, location: UserLocation?) =
      NavigationUiState.fromFerrostar(coreState, isMuted, location)
}
