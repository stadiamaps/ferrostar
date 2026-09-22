package com.stadiamaps.ferrostar.core.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.stadiamaps.ferrostar.core.NavigationState
import com.stadiamaps.ferrostar.core.NavigationStateObserver
import java.lang.ref.WeakReference

interface ForegroundServiceManager : NavigationStateObserver {

  fun startService(stopNavigation: () -> Unit)

  fun stopService()
}

/**
 * A manager for the FerrostarForegroundService. This class is responsible for starting and stopping
 * the service, as well as updating the navigation state. It effectively acts as a bridge between
 * [FerrostarCore] and the [FerrostarForegroundService].
 *
 * @param T The notification builder. This can be [DefaultForegroundNotificationBuilder] or a custom
 *   implementation of [ForegroundNotificationBuilder]. See the default for an example.
 */
class FerrostarForegroundServiceManager<T : ForegroundNotificationBuilder>(
    context: Context,
    private val notificationBuilder: T,
) : ForegroundServiceManager, ServiceConnection {

  companion object {
    const val TAG = "ForegroundServiceManager"
    const val CHANNEL_ID = "ferrostar_navigation"
  }

  private val weakContext: WeakReference<Context> = WeakReference(context)
  private val context: Context
    get() = weakContext.get() ?: throw IllegalStateException("Context is null")

  // True from startService() until stopService().
  private var isRequested = false

  private var service: FerrostarForegroundService? = null

  private var stopNavigating: (() -> Unit)? = null
  private val stopNavigationReceiver: BroadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          Log.d(TAG, "Stop navigation intent received. Invoking stopNavigating.")
          stopNavigating?.invoke()
        }
      }

  override fun startService(stopNavigation: () -> Unit) {
    // Close any earlier session first, so a second start never leaves a dangling bind.
    stopService()

    this.stopNavigating = stopNavigation
    isRequested = true

    // Build the intent to start the foreground service.
    val intent = Intent(context, FerrostarForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }

    notificationBuilder.channelId = CHANNEL_ID

    // Register the receivers for the notification intents.
    registerReceiver()

    // Bind the foreground service to this manager.
    context.bindService(intent, this, Context.BIND_AUTO_CREATE)

    Log.d(TAG, "Started foreground service")
  }

  override fun stopService() {
    if (!isRequested) {
      Log.d(TAG, "Service is not started. Ignoring stop request.")
      return
    }
    isRequested = false

    Log.d(TAG, "Stopping foreground service")
    context.unregisterReceiver(stopNavigationReceiver)
    // Unbinding is safe before the connection completes. It throws only when bindService never
    // ran, for example when startForegroundService was refused.
    try {
      context.unbindService(this)
    } catch (e: IllegalArgumentException) {
      Log.d(TAG, "Service was not bound. Skipping unbind.")
    }
    service?.stop()
    // Drop the reference so a late onNavigationStateUpdated cannot re-post the notification.
    service = null
    // Stop the started service through the context. This ends it whether or not the bind ever
    // connected, so an early stop cannot leave an orphaned foreground service.
    context.stopService(Intent(context, FerrostarForegroundService::class.java))
  }

  // Pending intents for the notification.

  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  private fun registerReceiver() {
    // Register the stop navigation receiver. It's important that this uses the Application context,
    // not another context like the ForegroundServiceManager's context.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(
          stopNavigationReceiver,
          IntentFilter(ForegroundNotificationBuilder.STOP_NAVIGATION_INTENT),
          Context.RECEIVER_EXPORTED,
      )
    } else {
      context.registerReceiver(
          stopNavigationReceiver,
          IntentFilter(ForegroundNotificationBuilder.STOP_NAVIGATION_INTENT),
      )
    }
  }

  // Methods for navigation state.

  override fun onNavigationStateUpdated(state: NavigationState) {
    // FerrostarCore calls stopService() before it cancels its location job, so an in-flight
    // update can land after a stop.
    if (!isRequested) return
    service?.onNavigationStateUpdated(state)
  }

  // Methods from ServiceConnection

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    val connected = (service as FerrostarForegroundService.LocalBinder).service
    if (!isRequested) {
      // The session stopped before the bind connected. Do not promote the service to foreground;
      // stopService() already asked the system to stop it.
      Log.d(TAG, "Service connected after stop. Ignoring.")
      return
    }
    this.service = connected

    // Set the notification builder for the service. This will be used to create the notification
    // using the UI library's default notification or a custom notification.
    connected.notificationBuilder = this.notificationBuilder
    connected.start()
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    this.service?.stop()
    this.service = null
  }
}
