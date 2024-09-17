package com.stadiamaps.ferrostar.googleplayservices

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.LocationUpdateListener
import com.stadiamaps.ferrostar.core.toUserLocation
import java.util.concurrent.Executor
import uniffi.ferrostar.Heading
import uniffi.ferrostar.UserLocation

class FusedLocationProvider(
    context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
) : LocationProvider {

  companion object {
    private const val TAG = "GooglePlayServicesLocationProvider"
  }

  override var lastLocation: UserLocation? = null
    private set

  override var lastHeading: Heading? = null
    private set

  private val listeners: MutableMap<LocationUpdateListener, LocationListener> = mutableMapOf()

  @SuppressLint("MissingPermission")
  override fun addListener(listener: LocationUpdateListener, executor: Executor) {
    Log.d(TAG, "Adding listener")
    if (listeners.contains(listener)) {
      Log.d(TAG, "Listener already added")
      return
    }

    val locationListener = LocationListener { newLocation ->
      listener.onLocationUpdated(newLocation.toUserLocation())
    }
    listeners[listener] = locationListener

    val locationRequest =
        LocationRequest.Builder(1000L).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()

    fusedLocationProviderClient.requestLocationUpdates(
        locationRequest, locationListener, Looper.getMainLooper())
  }

  override fun removeListener(listener: LocationUpdateListener) {
    val activeListener = listeners.remove(listener)

    if (activeListener != null) {
      fusedLocationProviderClient.removeLocationUpdates(activeListener)
    }
  }
}
