package com.stadiamaps.ferrostar.core.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class AndroidLocationProviderTest {
  private val mockContext = mockk<Context>()
  private val mockLocationManager = mockk<LocationManager>()
  private val mockLocation = mockk<Location>()

  @Before
  fun setup() {
    every { mockContext.getSystemService(Context.LOCATION_SERVICE) } returns mockLocationManager
  }

  @After
  fun teardown() {
    unmockkAll()
  }

  @Test
  fun `lastLocation returns null when no last known location exists`() = runTest {
    every { mockLocationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
    every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns null

    val provider = AndroidLocationProvider(mockContext)
    assertNull(provider.lastLocation())
  }

  @Test
  fun `lastLocation returns location from location manager`() = runTest {
    every { mockLocationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
    every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns
        mockLocation

    val provider = AndroidLocationProvider(mockContext)
    assertSame(mockLocation, provider.lastLocation())
  }

  @Test
  fun `getBestProvider prefers GPS over network`() = runTest {
    val networkLocation = mockk<Location>()
    every { mockLocationManager.getProviders(true) } returns
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns
        mockLocation
    every { mockLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } returns
        networkLocation

    val provider = AndroidLocationProvider(mockContext)
    assertSame(mockLocation, provider.lastLocation())
  }

  @Test
  fun `getBestProvider falls back to network when GPS unavailable`() = runTest {
    val networkLocation = mockk<Location>()
    every { mockLocationManager.getProviders(true) } returns
        listOf(LocationManager.NETWORK_PROVIDER)
    every { mockLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } returns
        networkLocation

    val provider = AndroidLocationProvider(mockContext)
    assertSame(networkLocation, provider.lastLocation())
  }

  @Test
  fun `getBestProvider falls back to passive when no other provider is available`() = runTest {
    every { mockLocationManager.getProviders(true) } returns emptyList()
    every { mockLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } returns
        null

    val provider = AndroidLocationProvider(mockContext)
    assertNull(provider.lastLocation())
  }

  @Test
  fun `locationUpdates emits last known location immediately on subscribe`() = runTest {
    val listenerSlot = slot<LocationListener>()
    every { mockLocationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
    every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns
        mockLocation
    every {
      mockLocationManager.requestLocationUpdates(
          any<String>(), any<Long>(), any<Float>(), capture(listenerSlot), any())
    } just Runs
    every { mockLocationManager.removeUpdates(any<LocationListener>()) } just Runs

    val provider = AndroidLocationProvider(mockContext)
    provider.locationUpdates().test {
      assertSame(mockLocation, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `locationUpdates emits when the location listener fires`() = runTest {
    val listenerSlot = slot<LocationListener>()
    val newLocation = mockk<Location>()
    every { mockLocationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
    every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns null
    every {
      mockLocationManager.requestLocationUpdates(
          any<String>(), any<Long>(), any<Float>(), capture(listenerSlot), any())
    } just Runs
    every { mockLocationManager.removeUpdates(any<LocationListener>()) } just Runs

    val provider = AndroidLocationProvider(mockContext)
    provider.locationUpdates().test {
      listenerSlot.captured.onLocationChanged(newLocation)
      assertSame(newLocation, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `locationUpdates unregisters listener when flow is cancelled`() = runTest {
    val listenerSlot = slot<LocationListener>()
    every { mockLocationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
    every { mockLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns null
    every {
      mockLocationManager.requestLocationUpdates(
          any<String>(), any<Long>(), any<Float>(), capture(listenerSlot), any())
    } just Runs
    every { mockLocationManager.removeUpdates(any<LocationListener>()) } just Runs

    val provider = AndroidLocationProvider(mockContext)
    provider.locationUpdates().test { cancelAndIgnoreRemainingEvents() }

    verify { mockLocationManager.removeUpdates(any<LocationListener>()) }
  }
}
