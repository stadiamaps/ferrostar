package com.stadiamaps.ferrostar.maplibreui.routeline

import java.util.logging.Logger
import uniffi.ferrostar.GeographicCoordinate

private val routeGeoJsonLogger: Logger = Logger.getLogger("RouteGeoJson")

internal fun lineStringFeatureCollectionJson(points: List<GeographicCoordinate>): String? {
  if (points.size < 2) {
    routeGeoJsonLogger.warning(
        "Skipping route line render because fewer than 2 points were provided."
    )
    return null
  }

  val coordinates = points.joinToString(separator = ",") { "[${it.lng},${it.lat}]" }
  return """
    {"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coordinates]},"properties":{}}]}
  """.trimIndent()
}
