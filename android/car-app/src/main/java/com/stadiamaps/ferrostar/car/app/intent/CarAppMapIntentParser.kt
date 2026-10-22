package com.stadiamaps.ferrostar.car.app.intent

import android.content.Intent
import androidx.car.app.CarContext
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntent.Navigate
import java.net.URLDecoder
import uniffi.ferrostar.GeographicCoordinate

/** Reads an [Intent] into a [CarAppMapIntent]. It does not geocode. */
object CarAppMapIntentParser {

  /** Returns the [CarAppMapIntent] for [intent], or null if the URI is missing or unknown. */
  fun parse(intent: Intent): CarAppMapIntent? {
    val uri = intent.data ?: return null
    return parse(intent.action, uri.scheme, uri.encodedSchemeSpecificPart)
  }

  /** Do not use `Uri.getQueryParameter` here. It throws on opaque URIs such as `geo:`. */
  internal fun parse(action: String?, scheme: String?, encodedSsp: String?): CarAppMapIntent? {
    val ssp = encodedSsp ?: return null
    return when (scheme?.lowercase()) {
      // The navigation app doc: NAVIGATE + geo: is navigation, VIEW + geo: is search.
      "geo" -> if (action in NAVIGATE_ACTIONS) parseGeoNavigate(ssp) else parseGeoView(ssp)
      "google.navigation" -> parseGoogleNavigation(queryParameters(ssp))
      "google.streetview" -> parseStreetView(queryParameters(ssp))
      else -> null
    }
  }

  // https://developer.android.com/develop/devices/assistant/intents-assistant-nav-app#navigation-intent
  private val NAVIGATE_ACTIONS = setOf(CarContext.ACTION_NAVIGATE, "android.intent.action.NAVIGATE")

  private val LABELED_COORDINATE =
      Regex(
          """^\s*([-+]?[0-9.]+)\s*,\s*([-+]?[0-9.]+)\s*(?:\((.*)\))?\s*$""",
          RegexOption.DOT_MATCHES_ALL,
      )

  private fun parseGeoView(ssp: String): CarAppMapIntent? {
    val params = queryParameters(ssp.substringAfter('?', ""))
    val zoom = params["z"]?.toIntOrNull()
    val q = params["q"]?.takeIf { it.isNotBlank() }
    val center = geoCoordinate(ssp)
    return when {
      q != null ->
          parseLabeledCoordinate(q)?.let { (location, label) ->
            CarAppMapIntent.Pin(location, label)
          } ?: CarAppMapIntent.Search(q, center, zoom)
      center != null -> CarAppMapIntent.DisplayMap(center, zoom)
      else -> null
    }
  }

  private fun parseGeoNavigate(ssp: String): Navigate? {
    val params = queryParameters(ssp.substringAfter('?', ""))
    val q = params["q"]?.takeIf { it.isNotBlank() }
    val center = geoCoordinate(ssp)
    val destination =
        when {
          center != null -> Navigate.Destination.Coordinate(center, q)
          q != null -> destination(q)
          else -> return null
        }
    return navigate(destination, params, action(params["intent"]))
  }

  private fun parseGoogleNavigation(params: Map<String, String>): Navigate? {
    val q = params["q"]?.takeIf { it.isNotBlank() } ?: return null
    return navigate(destination(q), params, Navigate.Action.NAVIGATE)
  }

  private fun parseStreetView(params: Map<String, String>): CarAppMapIntent.StreetView? {
    val panoramaId = params["panoid"]?.takeIf { it.isNotBlank() }
    if (panoramaId != null) return CarAppMapIntent.StreetView.Panorama(panoramaId)
    return params["cbll"]?.let(::parseCoordinate)?.let { CarAppMapIntent.StreetView.Location(it) }
  }

  private fun navigate(
      destination: Navigate.Destination,
      params: Map<String, String>,
      action: Navigate.Action,
  ) = Navigate(destination, action, travelMode(params["mode"]), avoid(params["avoid"]))

  private fun destination(q: String): Navigate.Destination =
      parseLabeledCoordinate(q)?.let { (location, label) ->
        Navigate.Destination.Coordinate(location, label)
      } ?: Navigate.Destination.Query(q)

  // A missing or unknown `intent` means navigation.
  private fun action(value: String?) =
      when (value) {
        "add_a_stop" -> Navigate.Action.ADD_STOP
        "directions" -> Navigate.Action.DIRECTIONS
        else -> Navigate.Action.NAVIGATE
      }

  private fun travelMode(value: String?) =
      when (value) {
        "d" -> Navigate.TravelMode.DRIVING
        "b" -> Navigate.TravelMode.BICYCLING
        "l" -> Navigate.TravelMode.TWO_WHEELER
        "w" -> Navigate.TravelMode.WALKING
        "x" -> Navigate.TravelMode.TAXI
        "r" -> Navigate.TravelMode.TRANSIT
        else -> null
      }

  private fun avoid(value: String?): Set<Navigate.Avoid> =
      value.orEmpty().mapNotNullTo(mutableSetOf()) {
        when (it) {
          't' -> Navigate.Avoid.TOLLS
          'h' -> Navigate.Avoid.HIGHWAYS
          'f' -> Navigate.Avoid.FERRIES
          else -> null
        }
      }

  /** The `lat,lng` part of a `geo:` URI. `geo:0,0` means "no coordinate". */
  private fun geoCoordinate(ssp: String): GeographicCoordinate? =
      // RFC 5870 allows `;u=…` and other parameters after the coordinate.
      parseCoordinate(ssp.substringBefore('?').substringBefore(';'))?.takeUnless {
        it.lat == 0.0 && it.lng == 0.0
      }

  /** `lat,lng` or `lat,lng(label)`, the form of `q` that a labeled pin uses. */
  private fun parseLabeledCoordinate(q: String): Pair<GeographicCoordinate, String?>? {
    val (lat, lng, label) = LABELED_COORDINATE.find(q)?.destructured ?: return null
    val location = parseCoordinate("$lat,$lng") ?: return null
    return location to label.trim().ifEmpty { null }
  }

  /** `lat,lng` with an optional third part (altitude) that this parser ignores. */
  private fun parseCoordinate(str: String): GeographicCoordinate? {
    val parts = str.split(",", limit = 3)
    if (parts.size < 2) return null
    val lat = parts[0].trim().toDoubleOrNull() ?: return null
    val lng = parts[1].trim().toDoubleOrNull() ?: return null
    // `in` rejects NaN. Do not change it to `<` and `>`.
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return GeographicCoordinate(lat, lng)
  }

  /**
   * Splits `key=value&key=value` and decodes each value. The first value for a key wins.
   *
   * [URLDecoder] decodes `+` as a space, as the
   * [doc](https://developer.android.com/guide/components/google-maps-intents#urlencoding) allows.
   */
  private fun queryParameters(query: String): Map<String, String> {
    val params = mutableMapOf<String, String>()
    for (pair in query.removePrefix("?").split('&')) {
      if (pair.isEmpty()) continue
      params.getOrPut(pair.substringBefore('=')) { decode(pair.substringAfter('=', "")) }
    }
    return params
  }

  // A value with a bad percent escape stays as it is.
  private fun decode(raw: String): String =
      try {
        URLDecoder.decode(raw, "UTF-8")
      } catch (e: IllegalArgumentException) {
        raw
      }
}
