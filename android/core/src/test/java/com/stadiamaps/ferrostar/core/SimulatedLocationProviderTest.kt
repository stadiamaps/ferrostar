package com.stadiamaps.ferrostar.core

import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Heading
import uniffi.ferrostar.UserLocation

class SimulatedLocationProviderTest {
  @Test
  fun `initial values are null`() {
    val locationProvider = SimulatedLocationProvider()

    assertNull(locationProvider.lastLocation)
    assertNull(locationProvider.lastHeading)
  }

  @Test
  fun `set location`() {
    val locationProvider = SimulatedLocationProvider()
    val location = UserLocation(GeographicCoordinate(42.02, 24.0), 12.0, null, Instant.now(), null)

    locationProvider.lastLocation = location

    assertEquals(locationProvider.lastLocation, location)
  }

  @Test
  fun `test listener events`() {
    val latch = CountDownLatch(1)
    val locationProvider = SimulatedLocationProvider()
    val location = UserLocation(GeographicCoordinate(42.02, 24.0), 12.0, null, Instant.now(), null)

    val listener =
        object : LocationUpdateListener {
          override fun onLocationUpdated(location: UserLocation) {
            assertEquals(location, location)

            latch.countDown()
          }

          override fun onHeadingUpdated(heading: Heading) {
            fail("Unexpected heading update")
          }
        }

    locationProvider.addListener(listener, Executors.newSingleThreadExecutor())

    locationProvider.lastLocation = location

    assertEquals(locationProvider.lastLocation, location)
    latch.await(1, TimeUnit.SECONDS)
  }
}
