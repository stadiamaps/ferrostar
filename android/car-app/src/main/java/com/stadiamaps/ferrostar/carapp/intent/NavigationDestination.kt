package com.stadiamaps.ferrostar.carapp.intent

/**
 * A parsed navigation destination from an external intent.
 *
 * At least one of [latitude]/[longitude] or [query] will be non-null when returned from
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
