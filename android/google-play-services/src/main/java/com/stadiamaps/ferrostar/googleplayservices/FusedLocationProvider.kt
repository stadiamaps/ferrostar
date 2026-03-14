package com.stadiamaps.ferrostar.googleplayservices

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

interface LocationProviding {
  suspend fun getLastLocation(
      priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
  ): Location?

  suspend fun getNextLocation(
      priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
      timeoutMillis: Long = 60000
  ): Location?

  fun locationUpdates(
      priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
      intervalMillis: Long = 1000
  ): Flow<Location>
}

class FusedLocationProvider(context: Context) : LocationProviding {
  private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

  @SuppressLint("MissingPermission")
  override suspend fun getLastLocation(priority: Int): Location? =
      suspendCoroutine { continuation ->
        Log.d(TAG, "Requesting last location")
        val requestStart = System.currentTimeMillis()

        fusedLocationClient
            .getLastLocation(LastLocationRequest.Builder().build())
            .addOnSuccessListener { location ->
              val durationSeconds = (System.currentTimeMillis() - requestStart) / 1000.0
              Log.d(TAG, "Obtained last location in $durationSeconds s")
              continuation.resume(location)
            }
            .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
      }

  // Get the next fresh location update with timeout
  @SuppressLint("MissingPermission")
  override suspend fun getNextLocation(priority: Int, timeoutMillis: Long): Location? =
      suspendCancellableCoroutine { continuation ->
        val requestStart = System.currentTimeMillis()
        Log.d(TAG, "Requesting next location with priority: $priority")
        val cancellationTokenSource = CancellationTokenSource()

        // https://developers.google.com/android/reference/com/google/android/gms/location/CurrentLocationRequest.Builder
        val request =
            CurrentLocationRequest.Builder()
                .setDurationMillis(timeoutMillis)
                .setPriority(priority)
                .build()

        fusedLocationClient
            .getCurrentLocation(request, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
              val durationSeconds = (System.currentTimeMillis() - requestStart) / 1000.0
              Log.d(TAG,"Obtained next location in $durationSeconds s")
              continuation.resume(location)
            }
            .addOnFailureListener { exception -> continuation.resumeWithException(exception) }

        continuation.invokeOnCancellation {
          val durationSeconds = (System.currentTimeMillis() - requestStart) / 1000.0
          Log.d(TAG,"Next location cancelled after $durationSeconds s")
          cancellationTokenSource.cancel()
        }
      }

  // Continuous location updates as Flow
  @SuppressLint("MissingPermission")
  override fun locationUpdates(priority: Int, intervalMillis: Long): Flow<Location> = callbackFlow {
    val request = LocationRequest.Builder(priority, intervalMillis).build()

    val callback =
        object : LocationCallback() {
          override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { trySend(it) }
          }
        }

    fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

    awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
  }

  companion object {
    private const val TAG = "LocationProvider"
  }
}
