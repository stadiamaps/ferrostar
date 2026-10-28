package com.stadiamaps.ferrostar.maplibreui.runtime

import kotlin.time.toKotlinInstant
import org.maplibre.compose.location.LocationMeasurement
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.degrees
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.meters
import uniffi.ferrostar.UserLocation

internal fun UserLocation.toLocationMeasurement(): LocationMeasurement =
    LocationMeasurement(
        position = Position(coordinates.lng, coordinates.lat),
        // The location indicator rejects a negative or non-finite accuracy radius.
        horizontalAccuracy = horizontalAccuracy.takeIf { it.isFinite() && it >= 0.0 }?.meters,
        distancePerSecond = speed?.value?.meters,
        distancePerSecondAccuracy = speed?.accuracy?.meters,
        course =
            courseOverGround?.let { course -> Bearing.North + course.degrees.toDouble().degrees },
        courseAccuracy = courseOverGround?.accuracy?.toDouble()?.degrees,
        measuredAt = timestamp.toKotlinInstant(),
    )

internal val LocationMeasurement.courseDegrees: Double?
  get() = course?.let { (it - Bearing.North).inDegrees }
