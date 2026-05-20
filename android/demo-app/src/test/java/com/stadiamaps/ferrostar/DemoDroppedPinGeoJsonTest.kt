package com.stadiamaps.ferrostar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.ferrostar.GeographicCoordinate

class DemoDroppedPinGeoJsonTest {
  @Test
  fun returnsNullWhenNoDroppedPinExists() {
    assertNull(droppedPinFeatureCollectionOrNull(null))
  }

  @Test
  fun storesDroppedPinInLongitudeLatitudeOrder() {
    val featureCollection = droppedPinFeatureCollection(GeographicCoordinate(48.2082, 16.3738))
    val point = featureCollection.features.single().geometry

    assertEquals(16.3738, point.longitude, 0.0)
    assertEquals(48.2082, point.latitude, 0.0)
  }

  @Test
  fun nullableHelperUsesLatestPinPosition() {
    val firstPin = GeographicCoordinate(48.2, 16.3)
    val secondPin = GeographicCoordinate(48.3, 16.4)

    val initialFeatureCollection = droppedPinFeatureCollectionOrNull(firstPin)
    val updatedFeatureCollection = droppedPinFeatureCollectionOrNull(secondPin)
    val initialPoint = initialFeatureCollection!!.features.single().geometry
    val updatedPoint = updatedFeatureCollection!!.features.single().geometry

    assertEquals(16.4, updatedPoint.longitude, 0.0)
    assertEquals(48.3, updatedPoint.latitude, 0.0)
    assertEquals(16.3, initialPoint.longitude, 0.0)
    assertEquals(48.2, initialPoint.latitude, 0.0)
    assertTrue(updatedPoint != initialPoint)
  }
}
