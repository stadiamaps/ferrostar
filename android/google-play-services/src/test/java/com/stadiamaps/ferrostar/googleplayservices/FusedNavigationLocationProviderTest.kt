package com.stadiamaps.ferrostar.googleplayservices

import android.content.Context
import android.location.Location
import com.google.android.gms.location.Priority
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class FusedNavigationLocationProviderTest {
  private val mockContext = mockk<Context>()
  private val mockLocationProvider = mockk<FusedLocationProvider>()
  private val mockLocation = mockk<Location>()

  @After
  fun teardown() {
    unmockkAll()
  }

  @Test
  fun `lastLocation delegates to getLastLocation with high accuracy priority`() = runTest {
    coEvery { mockLocationProvider.getLastLocation(Priority.PRIORITY_HIGH_ACCURACY) } returns
        mockLocation

    val provider = FusedNavigationLocationProvider(mockContext, mockLocationProvider)
    assertSame(mockLocation, provider.lastLocation())
  }

  @Test
  fun `lastLocation returns null when delegate returns null`() = runTest {
    coEvery { mockLocationProvider.getLastLocation(Priority.PRIORITY_HIGH_ACCURACY) } returns null

    val provider = FusedNavigationLocationProvider(mockContext, mockLocationProvider)
    assertNull(provider.lastLocation())
  }

  @Test
  fun `locationUpdates delegates to provider with high accuracy priority`() = runTest {
    val intervalMillis = 1000L
    val locationFlow = flowOf(mockLocation)
    every {
      mockLocationProvider.locationUpdates(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
    } returns locationFlow

    val provider = FusedNavigationLocationProvider(mockContext, mockLocationProvider)
    val result = provider.locationUpdates(intervalMillis)

    // Verify delegation to the underlying provider with the correct arguments
    verify { mockLocationProvider.locationUpdates(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis) }
    assertSame(locationFlow, result)
  }
}
