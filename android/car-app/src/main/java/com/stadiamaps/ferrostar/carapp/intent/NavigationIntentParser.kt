package com.stadiamaps.ferrostar.carapp.intent

import android.content.Intent
import android.net.Uri
import java.net.URLDecoder

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
    val scheme = uri.scheme ?: return null
    val ssp = uri.schemeSpecificPart ?: return null
    return parseSchemeSpecificPart(scheme, ssp)
  }

  companion object {
    /**
     * Parses a URI from its scheme and scheme-specific part strings.
     *
     * This is the pure-function core of the parser, usable without `android.net.Uri`.
     */
    fun parseSchemeSpecificPart(scheme: String, ssp: String): NavigationDestination? =
        when (scheme) {
          "geo" -> parseGeoSsp(ssp)
          "google.navigation" -> parseGoogleNavigationSsp(ssp)
          else -> null
        }

    /**
     * Parses the scheme-specific part of a `geo:` URI.
     *
     * Formats handled:
     * - `lat,lng`
     * - `0,0?q=lat,lng`
     * - `0,0?q=search+query`
     */
    fun parseGeoSsp(ssp: String): NavigationDestination? {
      val parts = ssp.split("?", limit = 2)

      val coords = parseCoordinates(parts[0])
      val query = if (parts.size > 1) parseQueryParam(parts[1], "q") else null

      // geo:0,0 is conventionally used as "no coordinates, use query instead"
      val hasCoordinates = coords != null && !(coords.first == 0.0 && coords.second == 0.0)

      return when {
        hasCoordinates -> NavigationDestination(coords!!.first, coords.second, query)
        query != null -> NavigationDestination(null, null, query)
        else -> null
      }
    }

    /**
     * Parses the scheme-specific part of a `google.navigation:` URI.
     *
     * Formats handled:
     * - `q=lat,lng`
     * - `q=place+name`
     */
    fun parseGoogleNavigationSsp(ssp: String): NavigationDestination? {
      val query = parseQueryParam(ssp, "q") ?: return null

      val coords = parseCoordinates(query)
      return if (coords != null) {
        NavigationDestination(coords.first, coords.second, null)
      } else {
        NavigationDestination(null, null, query)
      }
    }

    internal fun parseCoordinates(str: String): Pair<Double, Double>? {
      val parts = str.split(",", limit = 2)
      if (parts.size != 2) return null
      val lat = parts[0].trim().toDoubleOrNull() ?: return null
      val lng = parts[1].trim().toDoubleOrNull() ?: return null
      if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return null
      return lat to lng
    }

    internal fun parseQueryParam(queryString: String, key: String): String? {
      for (param in queryString.split("&")) {
        val kv = param.split("=", limit = 2)
        if (kv.size == 2 && kv[0] == key) {
          val decoded = URLDecoder.decode(kv[1], "UTF-8")
          return decoded.ifEmpty { null }
        }
      }
      return null
    }
  }
}
