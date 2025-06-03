package com.stadiamaps.ferrostar.core

import kotlin.math.max
import kotlin.math.min
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.getRoutePolyline

@Throws fun Route.getPolyline(precision: UInt): String = getRoutePolyline(this, precision)


/** A neutral bounding box type, which is not dependent on any particular map library. */
data class BoundingBox(
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double,
)

fun List<GeographicCoordinate>.boundingBox(): BoundingBox? =
    this.firstOrNull()?.let { start ->
      val initial =
          BoundingBox(north = start.lat, east = start.lng, south = start.lat, west = start.lng)

      fold(initial) { acc, current ->
        BoundingBox(
            north = max(acc.north, current.lat),
            east = max(acc.east, current.lng),
            south = min(acc.south, current.lat),
            west = min(acc.west, current.lng),
        )
      }
    }
