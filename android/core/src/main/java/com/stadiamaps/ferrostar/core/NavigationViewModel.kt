package com.stadiamaps.ferrostar.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.extensions.deviation
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.extensions.visualInstruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.SpokenInstruction
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.Waypoint
import kotlin.coroutines.CoroutineContext

data class NavigationUiState(
    val snappedLocation: UserLocation?,
    val heading: Float?,
    val routeGeometry: List<GeographicCoordinate>,
    val visualInstruction: VisualInstruction?,
    val spokenInstruction: SpokenInstruction?,
    val progress: TripProgress?,
    val isCalculatingNewRoute: Boolean?,
    val routeDeviation: RouteDeviation?
) {
  companion object {
    fun fromFerrostar(coreState: NavigationState, userLocation: UserLocation?): NavigationUiState =
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

  fun startNavigation(route: Route, config: NavigationControllerConfig? = null)
  fun replaceRoute(route: Route, config: NavigationControllerConfig? = null)
  fun stopNavigation()
}

class DefaultNavigationViewModel(
    private val ferrostarCore: FerrostarCore,
    locationProvider: LocationProvider
) : ViewModel(), NavigationViewModel {

  private var lastLocation: UserLocation? = locationProvider.lastLocation

  override val uiState =
      ferrostarCore.state
          .map { coreState ->
            lastLocation =
                when (coreState.tripState) {
                  is TripState.Navigating -> coreState.tripState.snappedUserLocation
                  is TripState.Complete,
                  TripState.Idle -> locationProvider.lastLocation
                }

            uiState(coreState, lastLocation)
            // This awkward dance is required because Kotlin doesn't have a way to map over
            // StateFlows without converting to a generic Flow in the process.
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              initialValue = uiState(ferrostarCore.state.value, lastLocation))

  private val _routes = MutableStateFlow<List<Route>?>(null)
  val routes: StateFlow<List<Route>?> = _routes.asStateFlow()

  fun getRoutes(
    origin: UserLocation,
    waypoints: List<Waypoint>
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      _routes.value = ferrostarCore.getRoutes(origin, waypoints)
    }
  }

  override fun startNavigation(route: Route, config: NavigationControllerConfig?) {
    ferrostarCore.startNavigation(route, config)
  }

  override fun replaceRoute(route: Route, config: NavigationControllerConfig?) {
    ferrostarCore.replaceRoute(route, config)
  }

  override fun stopNavigation() {
    ferrostarCore.stopNavigation()
  }

  private fun uiState(coreState: NavigationState, location: UserLocation?) =
      NavigationUiState.fromFerrostar(coreState, location)
}
