package com.stadiamaps.ferrostar

import android.location.Location
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.stadiamaps.ferrostar.core.DefaultNavigationViewModel
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.NavigationUiState
import com.stadiamaps.ferrostar.core.annotation.AnnotationPublisher
import com.stadiamaps.ferrostar.core.annotation.valhalla.valhallaExtendedOSRMAnnotationPublisher
import com.stadiamaps.ferrostar.core.location.NavigationLocationProvider
import com.stadiamaps.ferrostar.core.location.toUserLocation
import com.stadiamaps.ferrostar.support.initialSimulatedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

@OptIn(ExperimentalCoroutinesApi::class)
class DemoNavigationViewModel(
    // This is a simple example, but these would typically be dependency injected
    val ferrostarCore: FerrostarCore = AppModule.ferrostarCore,
    val locationProvider: NavigationLocationProvider = AppModule.locationProvider,
    annotationPublisher: AnnotationPublisher<*> = valhallaExtendedOSRMAnnotationPublisher()
) : DefaultNavigationViewModel(ferrostarCore, annotationPublisher) {

  private val _hasLocationPermission = MutableStateFlow(false)

  private val _simulated = MutableStateFlow(false)
  val simulated = _simulated.asStateFlow()

  private val locationStateFlow = MutableStateFlow<UserLocation?>(null)
  val location = locationStateFlow.asStateFlow()

  // Here's an example of injecting a custom location into the navigation UI state when isNavigating
  // is false.
  override val navigationUiState: StateFlow<NavigationUiState> =
      combine(super.navigationUiState, locationStateFlow) { a, b -> Pair(a, b) }
          .map { (uiState, location) ->
            if (uiState.isNavigating()) {
              uiState
            } else {
              uiState.copy(location = location)
            }
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(),
              initialValue =
                  NavigationUiState(
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      false,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null))

  init {
    viewModelScope.launch {
      _hasLocationPermission
          .flatMapLatest { hasPermission ->
            if (hasPermission) {
              locationProvider.locationUpdates(5000L)
                  .map { it.toUserLocation() }
            } else {
              flowOf(initialSimulatedLocation)
            }
          }
          .collect {
            locationStateFlow.emit(it)
          }
    }
  }

  fun setLocationPermissions(permitted: Boolean) {
    _hasLocationPermission.value = permitted
  }

  fun toggleSimulation() {
    _simulated.value = !_simulated.value
  }

  override fun toggleMute() {
    val spokenInstructionObserver = ferrostarCore.spokenInstructionObserver
    if (spokenInstructionObserver == null) {
      Log.d("NavigationViewModel", "Spoken instruction observer is null, mute operation ignored.")
      return
    }
    spokenInstructionObserver.setMuted(!spokenInstructionObserver.isMuted)
  }

  fun startNavigation(destination: Location) {
    viewModelScope.launch(Dispatchers.IO) {
      // TODO: Fail gracefully
      val lastLocation = location.value ?: return@launch

      val routes =
          ferrostarCore.getRoutes(
              lastLocation,
              listOf(
                  Waypoint(
                      coordinate =
                          GeographicCoordinate(destination.latitude, destination.longitude),
                      kind = WaypointKind.BREAK),
              ))

      val route = routes.first()

      if (simulated.value) {
        locationProvider.enableSimulationOn(route)
      }

      ferrostarCore.startNavigation(route = route)
    }
  }

  override fun stopNavigation() {
    locationProvider.disableSimulation()
    ferrostarCore.stopNavigation()
  }
}
