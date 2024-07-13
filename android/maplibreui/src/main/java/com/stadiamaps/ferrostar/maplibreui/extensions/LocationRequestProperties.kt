package com.stadiamaps.ferrostar.maplibreui.extensions

import com.maplibre.compose.ramani.LocationPriority
import com.maplibre.compose.ramani.LocationRequestProperties

/** Default location request properties for navigation. */
fun LocationRequestProperties.Companion.NavigationDefault(): LocationRequestProperties {
  return LocationRequestProperties.Builder()
      .priority(LocationPriority.PRIORITY_HIGH_ACCURACY)
      .interval(1000L)
      .fastestInterval(0L)
      .displacement(0F)
      .maxWaitTime(1000L)
      .build()
}
