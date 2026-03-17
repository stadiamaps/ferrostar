package com.stadiamaps.ferrostar.googleplayservices

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import app.cash.turbine.test
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class FusedLocationProviderTest {
  private val mockContext = mockk<Context>()
  private val mockFusedClient = mockk<FusedLocationProviderClient>()
  private val mockLocation = mockk<Location>()

  @Before
  fun setup() {
    mockkStatic(LocationServices::class)
    every { LocationServices.getFusedLocationProviderClient(mockContext) } returns mockFusedClient

    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0

    mockkStatic(Looper::class)
    every { Looper.getMainLooper() } returns mockk<Looper>(relaxed = true)
  }

  @After
  fun teardown() {
    unmockkAll()
  }

  // --- Helpers ---

  private fun successTask(location: Location?): Task<Location> {
    val task = mockk<Task<Location>>()
    every { task.addOnSuccessListener(any<OnSuccessListener<Location>>()) } answers {
      firstArg<OnSuccessListener<Location>>().onSuccess(location)
      task
    }
    every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
    return task
  }

  private fun failureTask(exception: Exception): Task<Location> {
    val task = mockk<Task<Location>>()
    every { task.addOnSuccessListener(any<OnSuccessListener<Location>>()) } returns task
    every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
      firstArg<OnFailureListener>().onFailure(exception)
      task
    }
    return task
  }

  private fun pendingTask(): Task<Location> {
    val task = mockk<Task<Location>>()
    every { task.addOnSuccessListener(any<OnSuccessListener<Location>>()) } returns task
    every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
    return task
  }

  // --- getLastLocation ---

  @Test
  fun `getLastLocation returns location on success`() = runTest {
    every { mockFusedClient.getLastLocation(any()) } returns successTask(mockLocation)

    val provider = FusedLocationProvider(mockContext)
    assertSame(mockLocation, provider.getLastLocation())
  }

  @Test
  fun `getLastLocation returns null when no location is cached`() = runTest {
    every { mockFusedClient.getLastLocation(any()) } returns successTask(null)

    val provider = FusedLocationProvider(mockContext)
    assertNull(provider.getLastLocation())
  }

  @Test(expected = RuntimeException::class)
  fun `getLastLocation throws on failure`() = runTest {
    every { mockFusedClient.getLastLocation(any()) } returns
        failureTask(RuntimeException("Location unavailable"))

    val provider = FusedLocationProvider(mockContext)
    provider.getLastLocation()
  }

  // --- getNextLocation ---

  @Test
  fun `getNextLocation returns location on success`() = runTest {
    every {
      mockFusedClient.getCurrentLocation(any<CurrentLocationRequest>(), any<CancellationToken>())
    } returns successTask(mockLocation)

    val provider = FusedLocationProvider(mockContext)
    assertSame(mockLocation, provider.getNextLocation())
  }

  @Test
  fun `getNextLocation cancels the token when the coroutine is cancelled`() = runTest {
    mockkConstructor(CancellationTokenSource::class)
    val mockToken = mockk<CancellationToken>()
    every { anyConstructed<CancellationTokenSource>().token } returns mockToken
    every { anyConstructed<CancellationTokenSource>().cancel() } just Runs
    every {
      mockFusedClient.getCurrentLocation(any<CurrentLocationRequest>(), any<CancellationToken>())
    } returns pendingTask()

    val provider = FusedLocationProvider(mockContext)
    val job = launch { provider.getNextLocation() }
    runCurrent() // let the coroutine reach the suspension point before cancelling
    job.cancel()
    job.join()

    verify { anyConstructed<CancellationTokenSource>().cancel() }
  }

  // --- locationUpdates ---

  @Test
  fun `locationUpdates emits when the location callback fires`() = runTest {
    val callbackSlot = slot<LocationCallback>()
    every {
      mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), any<Looper>())
    } returns mockk()
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val mockResult = mockk<LocationResult>()
    every { mockResult.lastLocation } returns mockLocation

    val provider = FusedLocationProvider(mockContext)
    provider.locationUpdates().test {
      callbackSlot.captured.onLocationResult(mockResult)
      assertSame(mockLocation, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `locationUpdates skips null locations from callback`() = runTest {
    val callbackSlot = slot<LocationCallback>()
    every {
      mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), any<Looper>())
    } returns mockk()
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val nullResult = mockk<LocationResult>()
    every { nullResult.lastLocation } returns null

    val validResult = mockk<LocationResult>()
    every { validResult.lastLocation } returns mockLocation

    val provider = FusedLocationProvider(mockContext)
    provider.locationUpdates().test {
      callbackSlot.captured.onLocationResult(nullResult) // should be dropped
      callbackSlot.captured.onLocationResult(validResult)
      assertSame(mockLocation, awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `locationUpdates unregisters callback when flow is cancelled`() = runTest {
    val callbackSlot = slot<LocationCallback>()
    every {
      mockFusedClient.requestLocationUpdates(any(), capture(callbackSlot), any<Looper>())
    } returns mockk()
    every { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) } returns mockk()

    val provider = FusedLocationProvider(mockContext)
    provider.locationUpdates().test { cancelAndIgnoreRemainingEvents() }

    verify { mockFusedClient.removeLocationUpdates(any<LocationCallback>()) }
  }
}
