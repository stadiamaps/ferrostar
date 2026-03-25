package com.stadiamaps.ferrostar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uniffi.ferrostar.GeographicCoordinate

class DemoDroppedPinGeoJsonTest {
  @Test
  fun returnsNullWhenNoDroppedPinExists() {
    assertNull(droppedPinFeatureCollectionJsonOrNull(null))
  }

  @Test
  fun serializesDroppedPinInLongitudeLatitudeOrder() {
    val json = droppedPinFeatureCollectionJson(GeographicCoordinate(48.2082, 16.3738))

    assertEquals(
        """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[16.3738,48.2082]},"properties":{}}]}""",
        json,
    )
  }

  @Test
  fun nullableHelperUsesLatestPinPosition() {
    val firstPin = GeographicCoordinate(48.2, 16.3)
    val secondPin = GeographicCoordinate(48.3, 16.4)

    val initialJson = droppedPinFeatureCollectionJsonOrNull(firstPin)
    val updatedJson = droppedPinFeatureCollectionJsonOrNull(secondPin)

    assertEquals(
        """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[16.4,48.3]},"properties":{}}]}""",
        updatedJson,
    )
    assertEquals(
        """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[16.3,48.2]},"properties":{}}]}""",
        initialJson,
    )
  }
}
