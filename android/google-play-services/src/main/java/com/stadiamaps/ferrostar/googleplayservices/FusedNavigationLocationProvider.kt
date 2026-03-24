package com.stadiamaps.ferrostar.googleplayservices

import android.content.Context
import android.location.Location
import com.google.android.gms.location.Priority
import com.stadiamaps.ferrostar.core.location.NavigationLocationProviding
import kotlinx.coroutines.flow.Flow

class FusedNavigationLocationProvider(
    context: Context,
    private val locationProvider: FusedLocationProvider = FusedLocationProvider(context)
): NavigationLocationProviding {
  override suspend fun lastLocation(): Location? =
      locationProvider.getLastLocation(Priority.PRIORITY_HIGH_ACCURACY)

  override fun locationUpdates(intervalMillis: Long): Flow<Location> =
      locationProvider.locationUpdates(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
}
