package com.stadiamaps.ferrostar.maplibreui.runtime

import java.time.Instant
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
  fun toMapLibreLocationMapsAllSupportedFields() {
    val location =
        UserLocation(
            coordinates = GeographicCoordinate(48.2082, 16.3738),
            horizontalAccuracy = 4.5,
            courseOverGround = CourseOverGround(degrees = 123.toUShort(), accuracy = 9.toUShort()),
            timestamp = Instant.EPOCH,
            speed = Speed(value = 13.4, accuracy = 0.8),
        )

    val mapLibreLocation = location.toMapLibreLocation()

    assertEquals(16.3738, mapLibreLocation.position.value.longitude, 0.0)
    assertEquals(48.2082, mapLibreLocation.position.value.latitude, 0.0)
    assertEquals(4.5, mapLibreLocation.position.accuracy!!.inMeters, 0.0)
    assertEquals(123.0, (mapLibreLocation.course!!.value - Bearing.North).inDegrees, 0.0)
    assertEquals(9.0, mapLibreLocation.course!!.accuracy!!.inDegrees, 0.0)
    assertEquals(13.4, mapLibreLocation.speed!!.distancePerSecond.inMeters, 0.0)
    assertEquals(0.8, mapLibreLocation.speed!!.accuracy!!.inMeters, 0.0)
  }

  @Test
  fun toMapLibreLocationLeavesOptionalFieldsNullWhenUnavailable() {
    val location =
        UserLocation(
            coordinates = GeographicCoordinate(48.2082, 16.3738),
            horizontalAccuracy = 12.0,
            courseOverGround = null,
            timestamp = Instant.EPOCH,
            speed = null,
        )

    val mapLibreLocation = location.toMapLibreLocation()

    assertNull(mapLibreLocation.course)
    assertNull(mapLibreLocation.speed)
  }

  @Test
  fun toMapLibreLocationPreservesBearingNearNorthWraparound() {
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

      val mapLibreLocation = location.toMapLibreLocation()

      assertEquals(bearing.toDouble(), mapLibreLocation.courseDegrees!!, 0.0)
    }
  }
}
