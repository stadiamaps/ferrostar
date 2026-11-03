package com.stadiamaps.ferrostar.core

import android.location.Location
import com.stadiamaps.ferrostar.core.location.NavigationLocationProviding
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavState
import uniffi.ferrostar.NavigationSession
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.TripState

class FerrostarCoreConcurrencyTest {
  @Test
  fun `stop leaves navigation idle after an in-flight location update`() {
    val fixture = Fixture()

    val state = fixture.duringLocationUpdate { fixture.core.stopNavigation() }

    assertEquals(NavigationState(), state)
  }

  @Test
  fun `an old location update cannot overwrite a restarted session`() {
    val fixture = Fixture()

    val state = fixture.duringLocationUpdate { fixture.core.startNavigation(fixture.nextRoute) }

    assertEquals(fixture.nextState, state)
  }

  @Test
  fun `an old location update cannot overwrite a replacement route`() {
    val fixture = Fixture()

    val state = fixture.duringLocationUpdate { fixture.core.replaceRoute(fixture.nextRoute) }

    assertEquals(fixture.nextState, state)
  }

  @Test
  fun `a cancelled collector waiting for the monitor cannot update a restarted session`() {
    val fixture = Fixture()

    val state = fixture.restartBeforeLocationUpdate()

    assertEquals(fixture.nextState, state)
  }

  private class Fixture {
    private val updateEntered = CountDownLatch(1)
    private val releaseUpdate = CountDownLatch(1)
    private val collectorFinished = CountDownLatch(1)
    private val collectorStarted = CountDownLatch(1)
    private val collectorThread = AtomicReference<Thread>()
    private val route = route(0.0)
    val nextRoute = route(2.0)
    private val initialState = navState()
    private val updatedState = navState()
    private val nextNavState = navState()
    val nextState = NavigationState(nextNavState.tripState, nextRoute.geometry)

    private val session =
        mockk<NavigationSession>(relaxUnitFun = true) {
          every { getInitialState(any()) } returns initialState
          every { updateUserLocation(any(), any()) } answers
              {
                updateEntered.countDown()
                check(releaseUpdate.await(10, TimeUnit.SECONDS)) { "Update was not released" }
                updatedState
              }
        }
    private val nextSession =
        mockk<NavigationSession>(relaxUnitFun = true) {
          every { getInitialState(any()) } returns nextNavState
          every { updateUserLocation(any(), any()) } returns updatedState
        }
    private val builder =
        mockk<FerrostarSessionBuilder> {
          every { build(route, null) } returns session
          every { build(nextRoute, null) } returns nextSession
        }
    private val location = mockk<Location>(relaxed = true)
    private val provider =
        mockk<NavigationLocationProviding> {
          every { locationUpdates(any()) } returns
              flow {
                try {
                  collectorThread.set(Thread.currentThread())
                  collectorStarted.countDown()
                  emit(location)
                } finally {
                  collectorFinished.countDown()
                }
              } andThen
              emptyFlow()
        }
    val core =
        FerrostarCore(
            routeProvider = RouteProvider.CustomProvider(mockk()),
            httpClient = mockk(),
            locationProvider = provider,
            navigationControllerConfig = mockk(),
            sessionBuilder = builder,
        )

    fun duringLocationUpdate(action: () -> Unit): NavigationState {
      val operation = FutureTask { action() }
      val thread = Thread(operation, "navigation-lifecycle-test")
      try {
        core.startNavigation(route)
        assertTrue("Location update did not start", updateEntered.await(10, TimeUnit.SECONDS))
        thread.start()

        // Let the lifecycle operation finish or block on the in-flight update before releasing
        // the native call. This forces the original race without depending on a sleep.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!operation.isDone && thread.state != Thread.State.BLOCKED) {
          check(System.nanoTime() < deadline) { "Lifecycle operation did not make progress" }
          Thread.yield()
        }
        releaseUpdate.countDown()
        operation.get(10, TimeUnit.SECONDS)
        assertTrue(
            "Location collector did not finish",
            collectorFinished.await(10, TimeUnit.SECONDS),
        )
        return core.state.value
      } finally {
        releaseUpdate.countDown()
        thread.join(10_000)
        core.stopNavigation()
      }
    }

    fun restartBeforeLocationUpdate(): NavigationState {
      try {
        synchronized(core) {
          core.startNavigation(route)
          assertTrue(
              "Location collector did not start",
              collectorStarted.await(10, TimeUnit.SECONDS),
          )
          val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
          while (collectorThread.get().state != Thread.State.BLOCKED) {
            check(System.nanoTime() < deadline) {
              "Location collector did not wait for the monitor"
            }
            Thread.yield()
          }
          core.startNavigation(nextRoute)
        }
        assertTrue(
            "Location collector did not finish",
            collectorFinished.await(10, TimeUnit.SECONDS),
        )
        verify(exactly = 0) { session.updateUserLocation(any(), any()) }
        verify(exactly = 0) { nextSession.updateUserLocation(any(), any()) }
        return core.state.value
      } finally {
        core.stopNavigation()
      }
    }

    private fun route(latitude: Double): Route {
      val geometry =
          listOf(GeographicCoordinate(latitude, 0.0), GeographicCoordinate(latitude + 1.0, 1.0))
      return mockk { every { this@mockk.geometry } returns geometry }
    }

    private fun navState(): NavState {
      val trip =
          mockk<TripState.Navigating> {
            every { deviation } returns RouteDeviation.NoDeviation
            every { spokenInstruction } returns null
          }
      return mockk { every { tripState } returns trip }
    }
  }
}
