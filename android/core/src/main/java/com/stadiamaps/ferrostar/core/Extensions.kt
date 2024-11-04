package com.stadiamaps.ferrostar.core

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.getRoutePolyline
import kotlin.math.max
import kotlin.math.min

@Throws fun Route.getPolyline(precision: UInt): String = getRoutePolyline(this, precision)

@Throws
fun RouteRequest.toOkhttp3Request(): Request {
  val headers: Map<String, String>
  return when (this) {
        is RouteRequest.HttpPost -> {
          headers = this.headers
          Request.Builder().url(url).post(body.toRequestBody())
        }

        is RouteRequest.HttpGet -> {
          headers = this.headers
          Request.Builder().url(url).get()
        }
      }
      .apply { headers.map { (name, value) -> header(name, value) } }
      .build()
}

/**
 * A neutral bounding box type, which is not dependent on any particular map library.
 */
data class BoundingBox(
  val north: Double,
  val east: Double,
  val south: Double,
  val west: Double,
)

fun List<GeographicCoordinate>.boundingBox(): BoundingBox? =
  this.firstOrNull()?.let { start ->
    val initial = BoundingBox(north = start.lat, east = start.lng, south = start.lat, west = start.lng)

    fold(initial) { acc, current ->
      BoundingBox(
        north = max(acc.north, current.lat),
        east = max(acc.east, current.lng),
        south = min(acc.south, current.lat),
        west = min(acc.west, current.lng),
      )
    }
  }
