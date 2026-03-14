package com.stadiamaps.ferrostar.core.location

import android.location.Location
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SimulatedLocationProviderTest {
  @Test
  fun `lastLocation is null when no route or initialLocation is set`() = runTest {
    val provider = SimulatedLocationProvider()
    assertNull(provider.lastLocation())
  }

  @Test
  fun `lastLocation returns initialLocation before any route is set`() = runTest {
    val location =
        Location("test").apply {
          latitude = 60.534716
          longitude = -149.543469
        }
    val provider = SimulatedLocationProvider(initialLocation = location)
    assertSame(location, provider.lastLocation())
  }
}
