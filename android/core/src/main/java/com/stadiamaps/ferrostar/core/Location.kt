package com.stadiamaps.ferrostar.core

import java.util.concurrent.Executor
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uniffi.ferrostar.Heading
import uniffi.ferrostar.LocationSimulationState
import uniffi.ferrostar.Route
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.advanceLocationSimulation
import uniffi.ferrostar.locationSimulationFromRoute

interface LocationProvider {
  val lastLocation: UserLocation?

  val lastHeading: Heading?

  fun addListener(listener: LocationUpdateListener, executor: Executor)

  fun removeListener(listener: LocationUpdateListener)
}

interface LocationUpdateListener {
  fun onLocationUpdated(location: UserLocation)

  fun onHeadingUpdated(heading: Heading)
}

/**
 * Location provider for testing without relying on simulator location spoofing.
 *
 * This allows for more granular unit tests.
 */
class SimulatedLocationProvider : LocationProvider {
  private var simulationState: LocationSimulationState? = null
  private val scope = CoroutineScope(Dispatchers.Default)
  private var simulationJob: Job? = null
  private var listeners: MutableList<Pair<LocationUpdateListener, Executor>> = mutableListOf()

  override var lastLocation: UserLocation? = null
    set(value) {
      field = value
      onLocationUpdated()
    }

  override var lastHeading: Heading? = null
    set(value) {
      field = value
      onHeadingUpdated()
    }

  /** A factor by which simulated route playback speed is multiplied. */
  var warpFactor: UInt = 1u

  override fun addListener(listener: LocationUpdateListener, executor: Executor) {
    listeners.add(listener to executor)

    if (simulationJob == null) {
      simulationJob = scope.launch { startSimulation() }
    }
  }

  override fun removeListener(listener: LocationUpdateListener) {
    listeners.removeIf { it.first == listener }

    if (listeners.isEmpty()) {
      simulationJob?.cancel()
    }
  }

  fun setSimulatedRoute(route: Route) {
    simulationState = locationSimulationFromRoute(route, resampleDistance = 10.0)

    if (listeners.isNotEmpty() && simulationJob == null) {
      simulationJob = scope.launch { startSimulation() }
    }
  }

  private suspend fun startSimulation() {
    while (true) {
      delay((1.0 / warpFactor.toFloat()).toDuration(DurationUnit.SECONDS))
      val initialState = simulationState ?: return
      val updatedState = advanceLocationSimulation(initialState)

      // Stop if the route has been fully simulated (no state change).
      if (updatedState == initialState) {
        return
      }

      simulationState = updatedState
      lastLocation = updatedState.currentLocation
    }
  }

  private fun onLocationUpdated() {
    val location = lastLocation
    if (location != null) {
      for ((listener, executor) in listeners) {
        executor.execute { listener.onLocationUpdated(location) }
      }
    }
  }

  private fun onHeadingUpdated() {
    val heading = lastHeading
    if (heading != null) {
      for ((listener, executor) in listeners) {
        executor.execute { listener.onHeadingUpdated(heading) }
      }
    }
  }
}

// TODO: Non-simulated implementations (GoogleFusedLocationProvider? AndroidSystemLocationProvider?)
// Put these in different modules!
