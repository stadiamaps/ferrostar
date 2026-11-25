package com.stadiamaps.ferrostar.maplibreui.runtime

import java.time.Instant
import kotlin.time.toKotlinInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.maplibre.spatialk.units.Bearing
import org.maplibre.spatialk.units.extensions.inDegrees
import org.maplibre.spatialk.units.extensions.inMeters
import uniffi.ferrostar.CourseOverGround
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Speed
import uniffi.ferrostar.UserLocation

class FerrostarLocationTest {
  @Test
  fun toLocationMeasurementMapsAllSupportedFields() {
    val timestamp = Instant.parse("2026-01-02T03:04:05.678Z")
    val location =
        UserLocation(
            coordinates = GeographicCoordinate(48.2082, 16.3738),
            horizontalAccuracy = 4.5,
            courseOverGround = CourseOverGround(degrees = 123.toUShort(), accuracy = 9.toUShort()),
            timestamp = timestamp,
            speed = Speed(value = 13.4, accuracy = 0.8),
        )

    val measurement = location.toLocationMeasurement()

    assertEquals(16.3738, measurement.position.longitude, 0.0)
    assertEquals(48.2082, measurement.position.latitude, 0.0)
    assertEquals(4.5, measurement.horizontalAccuracy!!.inMeters, 0.0)
    assertEquals(123.0, (measurement.course!! - Bearing.North).inDegrees, 0.0)
    assertEquals(9.0, measurement.courseAccuracy!!.inDegrees, 0.0)
    assertEquals(13.4, measurement.distancePerSecond!!.inMeters, 0.0)
    assertEquals(0.8, measurement.distancePerSecondAccuracy!!.inMeters, 0.0)
    assertEquals(timestamp.toKotlinInstant(), measurement.measuredAt)
  }

  @Test
  fun toLocationMeasurementLeavesOptionalFieldsNullWhenUnavailable() {
    val location =
        UserLocation(
            coordinates = GeographicCoordinate(48.2082, 16.3738),
            horizontalAccuracy = 12.0,
            courseOverGround = null,
            timestamp = Instant.EPOCH,
            speed = null,
        )

    val measurement = location.toLocationMeasurement()

    assertNull(measurement.course)
    assertNull(measurement.courseAccuracy)
    assertNull(measurement.distancePerSecond)
    assertNull(measurement.distancePerSecondAccuracy)
  }

  @Test
  fun toLocationMeasurementDropsInvalidHorizontalAccuracy() {
    listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { accuracy ->
      val location =
          UserLocation(
              coordinates = GeographicCoordinate(48.2082, 16.3738),
              horizontalAccuracy = accuracy,
              courseOverGround = null,
              timestamp = Instant.EPOCH,
              speed = null,
          )

      assertNull(location.toLocationMeasurement().horizontalAccuracy)
    }
  }

  @Test
  fun toLocationMeasurementKeepsZeroHorizontalAccuracy() {
    val location =
        UserLocation(
            coordinates = GeographicCoordinate(48.2082, 16.3738),
            horizontalAccuracy = 0.0,
            courseOverGround = null,
            timestamp = Instant.EPOCH,
            speed = null,
        )

    assertEquals(0.0, location.toLocationMeasurement().horizontalAccuracy!!.inMeters, 0.0)
  }

  @Test
  fun toLocationMeasurementPreservesBearingNearNorthWraparound() {
    val bearings = listOf(0.toUShort(), 1.toUShort(), 359.toUShort())

    bearings.forEach { bearing ->
      val location =
          UserLocation(
              coordinates = GeographicCoordinate(48.2082, 16.3738),
              horizontalAccuracy = 12.0,
              courseOverGround = CourseOverGround(degrees = bearing, accuracy = null),
              timestamp = Instant.EPOCH,
              speed = null,
          )

      val measurement = location.toLocationMeasurement()

      assertEquals(bearing.toDouble(), measurement.courseDegrees!!, 0.0)
    }
  }
}
