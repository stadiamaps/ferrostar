package com.stadiamaps.ferrostar.core

import org.junit.Test

import org.junit.Assert.*
import uniffi.ferrostar.GeographicCoordinates

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
}