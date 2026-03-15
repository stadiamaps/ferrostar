package com.stadiamaps.ferrostar.core

import app.cash.turbine.test
import com.stadiamaps.ferrostar.core.location.SimulatedLocationProvider
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.WellKnownRouteProvider

private const val valhallaEndpointUrl = "https://api.stadiamaps.com/navigate/v1"

class SimulatedLocationProviderTest {
  private fun parseRoute() =
      RouteAdapter.fromWellKnownRouteProvider(
              WellKnownRouteProvider.Valhalla(valhallaEndpointUrl, "auto"))
          .parseResponse(simpleRoute.trimIndent().toByteArray())
          .first()

  @Test
  fun locationUpdatesEmitsAfterSetRoute() = runTest {
    val provider = SimulatedLocationProvider(scope = backgroundScope, warpFactor = 8u)
    val route = parseRoute()

    provider.locationUpdates().test {
      provider.setRoute(route)

      val first = awaitItem()
      assertNotNull(first)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun lastLocationTracksRecentlyEmittedLocation() = runTest {
    val provider = SimulatedLocationProvider(scope = backgroundScope, warpFactor = 8u)
    val route = parseRoute()

    provider.locationUpdates().test {
      provider.setRoute(route)

      awaitItem() // first
      val second = awaitItem()

      assertEquals(second, provider.lastLocation())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun setRouteRestartsSimulationFromBeginning() = runTest {
    val provider = SimulatedLocationProvider(scope = backgroundScope, warpFactor = 8u)
    val route = parseRoute()
    val startCoord = route.geometry.first()

    provider.locationUpdates().test {
      provider.setRoute(route)

      // Advance a few steps into the simulation
      repeat(5) { awaitItem() }

      // Reset with the same route — simulation should restart from the beginning
      provider.setRoute(route)
      val restarted = awaitItem()

      assertEquals(startCoord.lat, restarted.latitude, 0.001)
      assertEquals(startCoord.lng, restarted.longitude, 0.001)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun multipleCollectorsReceiveSameLocationsViaSharedReplay() = runTest {
    val provider = SimulatedLocationProvider(scope = backgroundScope, warpFactor = 8u)
    val route = parseRoute()

    val collector1 = mutableListOf<android.location.Location>()
    val collector2 = mutableListOf<android.location.Location>()

    provider.setRoute(route)

    // Collect 3 items on each subscriber concurrently
    val job1 = launch { provider.locationUpdates().take(3).collect { collector1.add(it) } }
    val job2 = launch { provider.locationUpdates().take(3).collect { collector2.add(it) } }

    job1.join()
    job2.join()

    assertEquals(3, collector1.size)
    assertEquals(3, collector2.size)

    // Both collectors should have started from the same replayed position
    assertEquals(collector1.first().latitude, collector2.first().latitude, 0.0001)
    assertEquals(collector1.first().longitude, collector2.first().longitude, 0.0001)
  }

  @Test
  fun locationUpdatesEmitsNothingBeforeSetRoute() = runTest {
    val provider = SimulatedLocationProvider(scope = backgroundScope)
    assertNull(provider.lastLocation())

    provider.locationUpdates().test {
      // No route set — should not emit anything
      expectNoEvents()
      cancel()
    }
  }
}
