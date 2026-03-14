package com.stadiamaps.ferrostar.core.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface NavigationLocationProviding {
  suspend fun lastLocation(): Location?

  fun locationUpdates(intervalMillis: Long = 1000): Flow<Location>
}

