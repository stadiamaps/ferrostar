package com.stadiamaps.ferrostar.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.time.Instant
import java.util.concurrent.Executor
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uniffi.ferrostar.CourseOverGround
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Heading
import uniffi.ferrostar.LocationSimulationState
import uniffi.ferrostar.Route
import uniffi.ferrostar.Speed
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
 * A location provider that uses the Android system location services.
 *
 * NOTE: This does NOT attempt to check permissions. The caller is responsible for ensuring that
 * permissions are granted.
 */
class AndroidSystemLocationProvider(context: Context) : LocationProvider {
  companion object {
    private const val TAG = "AndroidLocationProvider"
  }

  override var lastLocation: UserLocation? = null
    private set

  override var lastHeading: Heading? = null
    private set

  val locationManager: LocationManager =
      context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
  private val listeners: MutableMap<LocationUpdateListener, LocationListener> = mutableMapOf()

  /**
   * Adds a location update listener.
   *
   * NOTE: This does NOT attempt to check permissions. The caller is responsible for ensuring that
   * permissions are enabled before calling this.
   */
  // TODO: This SuppressLint feels wrong; can't we push this "taint" up?
  @SuppressLint("MissingPermission")
  override fun addListener(listener: LocationUpdateListener, executor: Executor) {
    android.util.Log.d(TAG, "Add location listener")
    if (listeners.contains(listener)) {
      android.util.Log.d(TAG, "Already registered; skipping")
      return
    }
    val androidListener = LocationListener {
      val userLocation = it.toUserLocation()
      lastLocation = userLocation
      listener.onLocationUpdated(userLocation)
    }
    listeners[listener] = androidListener

    val handler = Handler(Looper.getMainLooper())

    executor.execute {
      handler.post {
        val last = locationManager.getLastKnownLocation(getBestProvider())
        last?.let { androidListener.onLocationChanged(last) }
        locationManager.requestLocationUpdates(getBestProvider(), 100L, 5.0f, androidListener)
      }
    }
  }

  override fun removeListener(listener: LocationUpdateListener) {
    android.util.Log.d(TAG, "Remove location listener")
    val androidListener = listeners.remove(listener)

    if (androidListener != null) {
      locationManager.removeUpdates(androidListener)
    }
  }

  private fun getBestProvider(): String {
    val providers = locationManager.getProviders(true).toSet()
    // Oh, how we love Android... Fused provider is brand new,
    // and we can't express this any other way than with duplicate clauses.
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      when {
        providers.contains(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
        providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> LocationManager.PASSIVE_PROVIDER
      }
    } else {
      when {
        providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> LocationManager.PASSIVE_PROVIDER
      }
    }
  }
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
    lastLocation = simulationState?.currentLocation

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

fun UserLocation.toAndroidLocation(): android.location.Location {
  val location = android.location.Location("FerrostarCore")

  location.latitude = this.coordinates.lat
  location.longitude = this.coordinates.lng
  location.accuracy = this.horizontalAccuracy.toFloat()

  // NOTE: We have a lot of checks in place which we could remove (+ improve correctness)
  // if we supported API 26.
  val course = this.courseOverGround
  if (course != null) {
    location.bearing = course.degrees.toFloat()

    val accuracy = course.accuracy
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && accuracy != null) {
      // NOTE: Course accuracy information is not available until API 26
      location.bearingAccuracyDegrees = accuracy.toFloat()
    }
  }

  location.time = this.timestamp.toEpochMilli()

  // FIXME: This is not entirely correct, but might be an acceptable approximation.
  // Feedback welcome as the purpose is not really documented.
  location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

  return location
}

fun android.location.Location.toUserLocation(): UserLocation {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    UserLocation(
        GeographicCoordinate(latitude, longitude),
        if (hasAccuracy()) {
          accuracy.toDouble()
        } else {
          Double.MAX_VALUE
        },
        if (hasBearing() && hasBearingAccuracy()) {
          CourseOverGround(bearing.toUInt().toUShort(), bearingAccuracyDegrees.toUInt().toUShort())
        } else {
          null
        },
        Instant.ofEpochMilli(time),
        if (hasSpeed() && hasSpeedAccuracy()) {
          Speed(speed.toDouble(), speedAccuracyMetersPerSecond.toDouble())
        } else {
          null
        })
  } else {
    UserLocation(
        GeographicCoordinate(latitude, longitude),
        if (hasAccuracy()) {
          accuracy.toDouble()
        } else {
          Double.MAX_VALUE
        },
        if (hasBearing()) {
          CourseOverGround(bearing.toUInt().toUShort(), null)
        } else {
          null
        },
        Instant.ofEpochMilli(time),
        if (hasSpeed()) {
          Speed(speed.toDouble(), null)
        } else {
          null
        })
  }
}
