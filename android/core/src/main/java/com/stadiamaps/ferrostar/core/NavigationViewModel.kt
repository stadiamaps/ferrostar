package com.stadiamaps.ferrostar.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.extensions.deviation
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.extensions.visualInstruction
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
    val snappedLocation: UserLocation,
    val heading: Float?,
    val routeGeometry: List<GeographicCoordinate>,
    val visualInstruction: VisualInstruction?,
    val spokenInstruction: SpokenInstruction?,
    val progress: TripProgress?,
    val isCalculatingNewRoute: Boolean?,
    val routeDeviation: RouteDeviation?
) {
  companion object {
    fun fromFerrostar(coreState: NavigationState, userLocation: UserLocation): NavigationUiState =
        NavigationUiState(
            snappedLocation = userLocation,
            // TODO: Heading/course over ground
            heading = null,
            routeGeometry = coreState.routeGeometry,
            visualInstruction = coreState.tripState.visualInstruction(),
            spokenInstruction = null,
            progress = coreState.tripState.progress(),
            isCalculatingNewRoute = coreState.isCalculatingNewRoute,
            routeDeviation = coreState.tripState.deviation())
  }
}

interface NavigationViewModel {
  val uiState: StateFlow<NavigationUiState>
  fun stopNavigation()
}

class DefaultNavigationViewModel(
    private val ferrostarCore: FerrostarCore,
    locationProvider: LocationProvider
) : ViewModel(), NavigationViewModel {

  private var lastLocation: UserLocation

  init {
    lastLocation =
        requireNotNull(locationProvider.lastLocation) {
          "LocationProvider must have a last location."
        }
  }

  override val uiState =
      ferrostarCore.state
          .map { coreState ->
            lastLocation =
                when (coreState.tripState) {
                  is TripState.Navigating -> coreState.tripState.snappedUserLocation
                  is TripState.Complete,
                  TripState.Idle -> lastLocation
                }

            uiState(coreState, lastLocation)
            // This awkward dance is required because Kotlin doesn't have a way to map over
            // StateFlows
            // without converting to a generic Flow in the process.
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              initialValue = uiState(ferrostarCore.state.value, lastLocation))

  override fun stopNavigation() {
    ferrostarCore.stopNavigation()
  }

  private fun uiState(coreState: NavigationState, location: UserLocation) =
      NavigationUiState.fromFerrostar(coreState, location)
}
