package com.stadiamaps.ferrostar.core

import java.util.concurrent.Executor
import uniffi.ferrostar.Heading
import uniffi.ferrostar.UserLocation

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

  override fun addListener(listener: LocationUpdateListener, executor: Executor) {
    listeners.add(listener to executor)
  }

  override fun removeListener(listener: LocationUpdateListener) {
    listeners.removeIf { it.first == listener }
  }

  private var listeners: MutableList<Pair<LocationUpdateListener, Executor>> = mutableListOf()

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
