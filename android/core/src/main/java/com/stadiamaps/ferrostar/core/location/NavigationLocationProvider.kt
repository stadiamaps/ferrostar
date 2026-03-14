package com.stadiamaps.ferrostar.core.location

import android.location.Location
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import uniffi.ferrostar.LocationBias
import uniffi.ferrostar.Route

class NavigationLocationProvider(
    private val liveProviding: NavigationLocationProviding,
    private val simulatedProvider: SimulatedLocationProvider
): NavigationLocationProviding {
  private val _isSimulating = MutableStateFlow(false)
  val isSimulating: StateFlow<Boolean>
    get() = _isSimulating.asStateFlow()

  fun enableSimulationOn(route: Route, bias: LocationBias = LocationBias.None) {
    simulatedProvider.setRoute(route, bias)
    _isSimulating.value = true
  }

  fun disableSimulation() {
    _isSimulating.value = false
  }

  override suspend fun lastLocation(): Location? =
    if (isSimulating.value) {
      simulatedProvider.lastLocation()
    } else {
      liveProviding.lastLocation()
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun locationUpdates(intervalMillis: Long): Flow<Location> =
    isSimulating
        .flatMapLatest { isSimulating ->
          if (isSimulating) {
            simulatedProvider.locationUpdates(intervalMillis)
          } else {
            liveProviding.locationUpdates(intervalMillis)
          }
        }
}
