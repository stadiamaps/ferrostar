package com.stadiamaps.ferrostar.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A location provider that uses the Android system location services (no Google Play Services
 * dependency).
 *
 * NOTE: This does NOT attempt to check permissions. The caller is responsible for ensuring that
 * location permissions are granted before use.
 */
class AndroidLocationProvider(context: Context) : NavigationLocationProviding {

  private val locationManager =
      context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

  @SuppressLint("MissingPermission")
  override suspend fun lastLocation(): Location? =
      locationManager.getLastKnownLocation(getBestProvider())

  @SuppressLint("MissingPermission")
  override fun locationUpdates(intervalMillis: Long): Flow<Location> = callbackFlow {
    val provider = getBestProvider()
    val listener = LocationListener { location -> trySend(location) }

    // Emit last known location immediately so the first update isn't delayed by the interval.
    locationManager.getLastKnownLocation(provider)?.let { trySend(it) }

    Log.d(TAG, "Requesting location updates from provider: $provider")
    locationManager.requestLocationUpdates(provider, intervalMillis, 0f, listener, Looper.getMainLooper())

    awaitClose {
      Log.d(TAG, "Removing location updates from provider: $provider")
      locationManager.removeUpdates(listener)
    }
  }

  private fun getBestProvider(): String {
    val providers = locationManager.getProviders(true).toSet()
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

  companion object {
    private const val TAG = "AndroidNavigationLocationProvider"
  }
}
