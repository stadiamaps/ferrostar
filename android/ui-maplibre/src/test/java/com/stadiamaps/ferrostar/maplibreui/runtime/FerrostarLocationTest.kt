package com.stadiamaps.ferrostar.maplibreui.runtime

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
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

    assertEquals(16.3738, mapLibreLocation.position.longitude, 0.0)
    assertEquals(48.2082, mapLibreLocation.position.latitude, 0.0)
    assertEquals(4.5, mapLibreLocation.accuracy, 0.0)
    assertEquals(123.0, mapLibreLocation.bearing!!, 0.0)
    assertEquals(9.0, mapLibreLocation.bearingAccuracy!!, 0.0)
    assertEquals(13.4, mapLibreLocation.speed!!, 0.0)
    assertEquals(0.8, mapLibreLocation.speedAccuracy!!, 0.0)
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

    assertNull(mapLibreLocation.bearing)
    assertNull(mapLibreLocation.bearingAccuracy)
    assertNull(mapLibreLocation.speed)
    assertNull(mapLibreLocation.speedAccuracy)
  }
}
