package com.stadiamaps.ferrostar.car.app.intent

import android.content.Intent
import android.net.Uri
import uniffi.ferrostar.GeographicCoordinate

/**
 * Parses navigation intents into [NavigationDestination] values.
 *
 * Supports two common URI schemes out of the box:
 * - `geo:lat,lng` and `geo:0,0?q=query` — standard Android geo URIs
 * - `google.navigation:q=lat,lng` and `google.navigation:q=place+name` — Google Maps URIs
 *
 * This class is `open` so apps can subclass to support additional URI schemes:
 * ```
 * class MyParser : NavigationIntentParser() {
 *     override fun parseUri(uri: Uri) = parseMyScheme(uri) ?: super.parseUri(uri)
 * }
 * ```
 */
open class NavigationIntentParser {

  /** Parses a navigation [Intent] into a [NavigationDestination], or null if unrecognized. */
  fun parse(intent: Intent): NavigationDestination? {
    val uri = intent.data ?: return null
    return parseUri(uri)
  }

  /** Parses a navigation [Uri] into a [NavigationDestination], or null if unrecognized. */
  open fun parseUri(uri: Uri): NavigationDestination? {
    // Uri.getQueryParameter() throws UnsupportedOperationException on opaque URIs (i.e. URIs
    // without an authority component, such as geo: and google.navigation:). Parse the
    // scheme-specific part directly instead.
    val ssp = uri.schemeSpecificPart ?: return null
    return when (uri.scheme) {
      "geo" ->
          parseGeoSsp(
              coordString = ssp.substringBefore('?'),
              query = ssp.substringAfter("?q=", "").ifEmpty { null }?.let { decodeQueryValue(it) })
      "google.navigation" ->
          ssp.substringAfter("q=", "").ifEmpty { null }?.let {
            parseGoogleNavigationSsp(decodeQueryValue(it))
          }
      else -> null
    }
  }

  companion object {
    /**
     * Parses the coordinate and optional query parts of a `geo:` URI.
     *
     * @param coordString The coordinate portion (before `?`), e.g. `"37.8,-122.4"` or `"0,0"`.
     *   Altitude is ignored if present (e.g. `"37.8,-122.4,100"`).
     * @param query The already-decoded value of the `q` parameter, if present.
     */
    fun parseGeoSsp(coordString: String, query: String?): NavigationDestination? {
      val coords = parseCoordinates(coordString)
      // geo:0,0 is conventionally used as "no coordinates, use query instead"
      val hasCoordinates = coords != null && !(coords.lat == 0.0 && coords.lng == 0.0)

      return when {
        hasCoordinates -> NavigationDestination(coords!!.lat, coords.lng, query)
        query != null -> NavigationDestination(null, null, query)
        else -> null
      }
    }

    /**
     * Parses the already-decoded `q` value from a `google.navigation:` URI.
     *
     * @param q The decoded value of the `q` parameter, e.g. `"37.8,-122.4"` or `"Starbucks"`.
     */
    fun parseGoogleNavigationSsp(q: String): NavigationDestination? {
      val coords = parseCoordinates(q)
      return if (coords != null) {
        NavigationDestination(coords.lat, coords.lng, null)
      } else {
        NavigationDestination(null, null, q)
      }
    }

    /**
     * Decodes a query parameter value extracted from an opaque URI's scheme-specific part.
     *
     * Handles both percent-encoding and `+` as space, matching the behavior of
     * [Uri.getQueryParameter] on hierarchical URIs.
     */
    internal fun decodeQueryValue(encoded: String): String =
        Uri.decode(encoded.replace("+", "%20"))

    internal fun parseCoordinates(str: String): GeographicCoordinate? {
      // limit=3 so altitude (geo:lat,lng,alt per RFC 5870) is captured and ignored
      val parts = str.split(",", limit = 3)
      if (parts.size < 2) return null
      val lat = parts[0].trim().toDoubleOrNull() ?: return null
      val lng = parts[1].trim().toDoubleOrNull() ?: return null
      if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return null
      return GeographicCoordinate(lat, lng)
    }
  }
}
