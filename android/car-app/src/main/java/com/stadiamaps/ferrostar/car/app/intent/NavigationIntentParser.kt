package com.stadiamaps.ferrostar.car.app.intent

import android.content.Intent
import android.net.Uri
import java.net.URLDecoder
import uniffi.ferrostar.GeographicCoordinate

/**
 * Parses navigation intents into [NavigationDestination] values.
 *
 * Supports two common URI schemes out of the box:
 * - `geo:lat,lng`, `geo:0,0?q=query` and `geo:0,0?q=lat,lng(label)` — standard Android geo URIs
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

  /**
   * Parses a navigation [Uri] into a [NavigationDestination], or null if unrecognized.
   *
   * `geo:` and `google.navigation:` URIs are opaque, so [Uri.getQueryParameter] throws on them.
   * This method reads the encoded scheme-specific part and parses it as a string instead.
   */
  open fun parseUri(uri: Uri): NavigationDestination? =
      parseSchemeSpecificPart(uri.scheme, uri.encodedSchemeSpecificPart)

  companion object {
    private val LABELED_COORDINATES =
        Regex("""^\s*(-?[0-9.]+)\s*,\s*(-?[0-9.]+)\s*(?:\((.*)\))?\s*$""")

    /**
     * Parses a navigation URI from its scheme and its encoded scheme-specific part.
     *
     * @param scheme The URI scheme, e.g. `"geo"` or `"google.navigation"`.
     * @param encodedSsp The encoded part after the scheme, e.g. `"0,0?q=coffee+shops"`.
     */
    fun parseSchemeSpecificPart(scheme: String?, encodedSsp: String?): NavigationDestination? {
      val ssp = encodedSsp ?: return null
      return when (scheme) {
        "geo" ->
            parseGeoSsp(
                coordString = ssp.substringBefore('?'),
                query = queryParameter(ssp.substringAfter('?', ""), "q"),
            )
        "google.navigation" -> queryParameter(ssp, "q")?.let { parseGoogleNavigationSsp(it) }
        else -> null
      }
    }

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
        query != null -> parseLabeledCoordinates(query) ?: NavigationDestination(null, null, query)
        else -> null
      }
    }

    /**
     * Parses the already-decoded `q` value from a `google.navigation:` URI.
     *
     * @param q The decoded value of the `q` parameter, e.g. `"37.8,-122.4"` or `"Starbucks"`.
     */
    fun parseGoogleNavigationSsp(q: String): NavigationDestination? =
        parseLabeledCoordinates(q) ?: NavigationDestination(null, null, q)

    /**
     * Parses a `q` value of the form `lat,lng` or `lat,lng(label)`, or returns null when the value
     * is not a coordinate pair.
     */
    internal fun parseLabeledCoordinates(q: String): NavigationDestination? {
      val match = LABELED_COORDINATES.find(q) ?: return null
      val (lat, lng, label) = match.destructured
      val coords = parseCoordinates("$lat,$lng") ?: return null
      return NavigationDestination(coords.lat, coords.lng, label.trim().ifEmpty { null })
    }

    /**
     * Returns the decoded value of [name] in a `key=value&key=value` query string, or null when the
     * key is absent. A value that fails to decode is returned as-is.
     */
    internal fun queryParameter(query: String, name: String): String? {
      if (query.isEmpty()) return null
      return query.split('&').firstNotNullOfOrNull { pair ->
        if (pair.substringBefore('=') != name) return@firstNotNullOfOrNull null
        val raw = pair.substringAfter('=', "")
        try {
          URLDecoder.decode(raw, "UTF-8")
        } catch (e: IllegalArgumentException) {
          raw
        }
      }
    }

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
