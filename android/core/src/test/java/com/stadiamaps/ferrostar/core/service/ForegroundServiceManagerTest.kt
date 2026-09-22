package com.stadiamaps.ferrostar.core.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.stadiamaps.ferrostar.core.NavigationState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import uniffi.ferrostar.TripState

class ForegroundServiceManagerTest {

  private val context = mockk<Context>(relaxed = true)
  private val notificationBuilder =
      object : ForegroundNotificationBuilder(context) {
        override fun build(tripState: TripState?): Notification = mockk()
      }
  private lateinit var manager: FerrostarForegroundServiceManager<ForegroundNotificationBuilder>

  @Before
  fun setUp() {
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    mockkConstructor(Intent::class)
    mockkConstructor(IntentFilter::class)
    manager = FerrostarForegroundServiceManager(context, notificationBuilder)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun bindAndConnect(): FerrostarForegroundService {
    val service = mockk<FerrostarForegroundService>(relaxed = true)
    val binder = mockk<FerrostarForegroundService.LocalBinder>()
    every { binder.service } returns service
    manager.onServiceConnected(null, binder)
    return service
  }

  @Test
  fun `stopService before the bind connects still stops the started service`() {
    manager.startService {}

    manager.stopService()

    verify(exactly = 1) { context.unbindService(manager) }
    verify(exactly = 1) { context.stopService(any()) }
  }

  @Test
  fun `a connection that arrives after stopService does not start the service`() {
    manager.startService {}
    manager.stopService()

    val service = bindAndConnect()

    verify(exactly = 0) { service.start() }
  }

  @Test
  fun `stopService after connection stops the service and drops the reference`() {
    manager.startService {}
    val service = bindAndConnect()

    manager.stopService()
    manager.onNavigationStateUpdated(NavigationState())

    verify(exactly = 1) { service.stop() }
    verify(exactly = 0) { service.onNavigationStateUpdated(any()) }
    verify(exactly = 1) { context.stopService(any()) }
  }

  @Test
  fun `state updates reach the connected service while running`() {
    manager.startService {}
    val service = bindAndConnect()

    manager.onNavigationStateUpdated(NavigationState())

    verify(exactly = 1) { service.onNavigationStateUpdated(any()) }
  }

  @Test
  fun `stopService without a start is a no-op`() {
    manager.stopService()

    verify(exactly = 0) { context.unbindService(any()) }
    verify(exactly = 0) { context.stopService(any()) }
  }

  @Test
  fun `a second start closes the first session before it binds again`() {
    manager.startService {}

    manager.startService {}

    verify(exactly = 1) { context.unbindService(manager) }
    verify(exactly = 2) { context.bindService(any(), manager, any<Int>()) }
  }
}
