package com.stadiamaps.ferrostar.maplibreui

import com.stadiamaps.ferrostar.core.NavigationUiState
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.UserLocation
import java.time.Instant

class NavigationPuckOverlayTest {
  @Test
  fun rendersOnlyWhileNavigatingWithLocation() {
    assertFalse(shouldRenderNavigationPuck(NavigationUiState.empty()))

    val location = sampleLocation()

    assertFalse(shouldRenderNavigationPuck(NavigationUiState.empty().copy(location = location)))
    assertTrue(
        shouldRenderNavigationPuck(
            NavigationUiState.empty().copy(
                progress = sampleProgress(),
                location = location,
            ),
        ),
    )
  }

  @Test
  fun fallsBackToLastKnownBearingWhenCurrentBearingMissing() {
    assertEquals(87.0, navigationPuckBearingDegrees(null, lastKnownBearing = 87.0), 0.0)
    assertEquals(42.0, navigationPuckBearingDegrees(42.0, lastKnownBearing = 87.0), 0.0)
  }

  @Test
  fun emitsPointGeoJsonWithBearingProperty() {
    val featureCollection = navigationPuckFeatureCollection(16.37, 48.21, 123.0)
    val feature = featureCollection.features.single()
    val bearing = feature.properties["bearing"]?.jsonPrimitive?.content?.toDouble()

    assertEquals(16.37, feature.geometry.longitude, 0.0)
    assertEquals(48.21, feature.geometry.latitude, 0.0)
    assertEquals(123.0, requireNotNull(bearing), 0.0)
  }

  private fun sampleLocation() =
      UserLocation(
          coordinates = GeographicCoordinate(48.21, 16.37),
          horizontalAccuracy = 5.0,
          courseOverGround = null,
          timestamp = Instant.now(),
          speed = null,
      )

  private fun sampleProgress() =
      TripProgress(
          distanceToNextManeuver = 1.0,
          distanceRemaining = 1.0,
          durationRemaining = 1.0,
      )
}
