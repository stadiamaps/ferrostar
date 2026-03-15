package com.stadiamaps.ferrostar.car.app.intent

import android.location.Location
import uniffi.ferrostar.GeographicCoordinate

/**
 * A parsed navigation destination from an external intent.
 *
 * At least one of [coordinate] or [query] will be non-null when returned from
 * [NavigationIntentParser].
 *
 * @param latitude Destination latitude, or null if only a query string is available.
 * @param longitude Destination longitude, or null if only a query string is available.
 * @param query Human-readable search query or place name, or null if only coordinates are
 *   available.
 */
data class NavigationDestination(
    val latitude: Double?,
    val longitude: Double?,
    val query: String?
) {
  /** The destination as a [Location], or null if only a query is available. */
  val location: Location?
    get() =
        if (latitude != null && longitude != null) {
          Location("").also {
            it.latitude = latitude
            it.longitude = longitude
          }
        } else {
          null
        }

  /** A human-readable display name for this destination. */
  val displayName: String
    get() =
        query
            ?: if (latitude != null && longitude != null) {
              "%.4f, %.4f".format(latitude, longitude)
            } else {
              "Unknown location"
            }
}
