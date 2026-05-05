package com.stadiamaps.ferrostar.maplibreui.routeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uniffi.ferrostar.GeographicCoordinate

class RouteGeoJsonTest {
  @Test
  fun returnsNullWhenTooFewPointsExist() {
    assertNull(lineStringFeatureCollectionJson(listOf(GeographicCoordinate(48.2, 16.3))))
  }

  @Test
  fun serializesLineStringInLongitudeLatitudeOrder() {
    val json =
        lineStringFeatureCollectionJson(
            listOf(
                GeographicCoordinate(48.2, 16.3),
                GeographicCoordinate(48.3, 16.4),
            ))

    assertEquals(
        """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[[16.3,48.2],[16.4,48.3]]},"properties":{}}]}""",
        json,
    )
  }
}
