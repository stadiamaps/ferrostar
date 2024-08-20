package com.stadiamaps.ferrostar.core.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.stadiamaps.ferrostar.core.NavigationState
import com.stadiamaps.ferrostar.core.NavigationStateObserver
import java.lang.ref.WeakReference

interface ForegroundServiceManager : NavigationStateObserver {
  fun startService()

  fun stopService()
}

class FerrostarForegroundServiceManager<T : ForegroundNotificationBuilder>(
  context: Context,
  private val notificationBuilder: T
) : ForegroundServiceManager, ServiceConnection {

  companion object {
    const val TAG = "ForegroundServiceManager"
  }

  private val weakContext: WeakReference<Context> = WeakReference(context)

  private var service: FerrostarForegroundService? = null

  override fun startService() {
    val context = weakContext.get() ?: throw IllegalStateException("Context is null")
    val intent = Intent(context, FerrostarForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }

    // Bind the foreground service to this manager.
    context.bindService(intent, this, Context.BIND_AUTO_CREATE)
  }

  override fun stopService() {
    val context = weakContext.get() ?: throw IllegalStateException("Context is null")
    context.unbindService(this)
  }

  // Methods for navigation state.

  override fun onNavigationState(state: NavigationState) {
    service?.onNavigationState(state)
  }

  // Methods from ServiceConnection

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    this.service = (service as FerrostarForegroundService.LocalBinder).service
    if (this.service == null) {
      throw IllegalStateException("FerrostarForegroundService is null")
    }

    // Set the notification builder for the service. This will be used to create the notification
    // using the UI library's default notification or a custom notification.
    this.service!!.notificationBuilder = this.notificationBuilder

    Log.d(TAG, "onServiceConnected - starting service")
    this.service!!.start()
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    Log.d(TAG, "onServiceDisconnected - cleaning up")
    this.service?.stop()
    this.service = null
  }
}
