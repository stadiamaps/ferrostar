package com.stadiamaps.ferrostar.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.SpokenInstruction
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.VisualInstruction

data class NavigationUiState(
    val snappedLocation: UserLocation,
    val heading: Float?,
    val routeGeometry: List<GeographicCoordinate>,
    val visualInstruction: VisualInstruction?,
    val spokenInstruction: SpokenInstruction?,
    val distanceToNextManeuver: Double?,
    val isCalculatingNewRoute: Boolean?,
    val routeDeviation: RouteDeviation?
)

class NavigationViewModel(
    stateFlow: StateFlow<FerrostarCoreState>,
    initialUserLocation: UserLocation,
) : ViewModel() {
  private var lastLocation: UserLocation = initialUserLocation

  val uiState =
      stateFlow
          .map { coreState ->
            lastLocation =
                when (coreState.tripState) {
                  is TripState.Navigating -> coreState.tripState.snappedUserLocation
                  is TripState.Complete -> lastLocation
                }

            uiState(coreState, lastLocation)
            // This awkward dance is required because Kotlin doesn't have a way to map over
            // StateFlows
            // without converting to a generic Flow in the process.
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              initialValue = uiState(stateFlow.value, initialUserLocation))

  private fun uiState(coreState: FerrostarCoreState, location: UserLocation) =
      NavigationUiState(
          snappedLocation = location,
          // TODO: Heading/course over ground
          heading = null,
          routeGeometry = coreState.routeGeometry,
          visualInstruction = visualInstructionForState(coreState.tripState),
          spokenInstruction = null,
          distanceToNextManeuver = distanceForState(coreState.tripState),
          isCalculatingNewRoute = coreState.isCalculatingNewRoute,
          routeDeviation = deviationForState(coreState.tripState))
}

private fun distanceForState(newState: TripState) =
    when (newState) {
      is TripState.Navigating -> newState.progress.distanceToNextManeuver
      is TripState.Complete -> null
    }

private fun visualInstructionForState(newState: TripState) =
    try {
      when (newState) {
        is TripState.Navigating -> newState.visualInstruction
        is TripState.Complete -> null
      }
    } catch (_: NoSuchElementException) {
      null
    }

private fun deviationForState(newState: TripState) =
    when (newState) {
      is TripState.Navigating -> newState.deviation
      is TripState.Complete -> null
    }
