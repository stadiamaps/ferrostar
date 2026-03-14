package com.stadiamaps.ferrostar.core.location

import android.location.Location
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import uniffi.ferrostar.LocationBias
import uniffi.ferrostar.LocationSimulationState
import uniffi.ferrostar.Route
import uniffi.ferrostar.advanceLocationSimulation
import uniffi.ferrostar.locationSimulationFromRoute

class SimulatedLocationProvider(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    /** A factor by which simulated route playback speed is multiplied. */
    var warpFactor: UInt = 1u,
    initialLocation: Location? = null
) : NavigationLocationProviding {

  // Emitting a new value here restarts the simulation from the beginning of the new route.
  private val _routeFlow = MutableStateFlow<LocationSimulationState?>(null)

  // Tracks current position within the active simulation run, seeded with initialLocation
  // so lastLocation() returns a sensible value before any route has been simulated.
  private var _lastLocation: Location? = initialLocation

  @OptIn(ExperimentalCoroutinesApi::class)
  private val sharedUpdates: Flow<Location> =
      _routeFlow
          .flatMapLatest { initialState ->
            // Capture into a local val so the non-null smart cast carries into the nested
            // flow lambda — parameter smart casts don't cross lambda boundaries.
            val startState: LocationSimulationState = initialState ?: return@flatMapLatest emptyFlow()
            flow {
              var state = startState
              var pendingCompletion = false

              while (true) {
                delay((1.0 / warpFactor.toFloat()).toDuration(DurationUnit.SECONDS))
                val updatedState = advanceLocationSimulation(state)

                // Stop if the route has been fully simulated (no state change).
                if (updatedState == state) {
                  if (pendingCompletion) {
                    return@flow
                  } else {
                    pendingCompletion = true
                  }
                }

                state = updatedState
                val loc = updatedState.currentLocation.toAndroidLocation()
                _lastLocation = loc
                emit(loc)
              }
            }
          }
          .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

  fun setRoute(route: Route, bias: LocationBias = LocationBias.None) {
    _routeFlow.value = locationSimulationFromRoute(route, resampleDistance = 10.0, bias)
  }

  override suspend fun lastLocation(): Location? =
      _lastLocation ?: _routeFlow.value?.currentLocation?.toAndroidLocation()

  override fun locationUpdates(intervalMillis: Long): Flow<Location> = sharedUpdates
}
