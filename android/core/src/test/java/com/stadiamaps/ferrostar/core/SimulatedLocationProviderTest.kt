package com.stadiamaps.ferrostar.core

import org.junit.Test

import org.junit.Assert.*
import uniffi.ferrostar.GeographicCoordinates
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
        val location = SimulatedLocation(GeographicCoordinates(42.02, 24.0), 12.0f, null)

        locationProvider.lastLocation = location

        assertEquals(locationProvider.lastLocation, location)
    }

    @Test
    fun `test listener events`() {
        val latch = CountDownLatch(1)
        val locationProvider = SimulatedLocationProvider()
        val location = SimulatedLocation(GeographicCoordinates(42.02, 24.0), 12.0f, null)

        val listener = object : LocationUpdateListener {
            override fun onLocationUpdated(location: Location) {
                assertEquals(location, location)

                latch.countDown()
            }

            override fun onHeadingUpdated(heading: Float) {
                fail("Unexpected heading update")
            }
        }

        locationProvider.addListener(listener, Executors.newSingleThreadExecutor())

        locationProvider.lastLocation = location

        assertEquals(locationProvider.lastLocation, location)
        latch.await(1, TimeUnit.SECONDS)
    }
}