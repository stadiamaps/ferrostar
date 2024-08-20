package com.stadiamaps.ferrostar.core.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.stadiamaps.ferrostar.core.NavigationState
import com.stadiamaps.ferrostar.core.NavigationStateObserver
import com.stadiamaps.ferrostar.core.R
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.extensions.visualInstruction

class FerrostarForegroundService : Service(), NavigationStateObserver {

  companion object {
    const val TAG = "FerrostarForegroundService"
    const val CHANNEL_ID = "ferrostar_navigation"
    const val NOTIFICATION_ID = 501

    // Notification intents
    const val INTENT_FLAGS: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    const val STOP_NAVIGATION_INTENT = "com.ferrostar.intent.action.STOP_NAVIGATION"
    const val OPEN_NAVIGATION_INTENT = "com.ferrostar.intent.action.OPEN_NAVIGATION"
  }

  inner class LocalBinder : Binder() {
    val service: FerrostarForegroundService
      get() = this@FerrostarForegroundService
  }

  private val binder = LocalBinder()

  private val notificationManager: NotificationManager by lazy {
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private val stopNavigationReceiver: BroadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          // TODO: Stop the navigation service?
          Log.d("StopNavigationReceiver", "Stop navigation?!?!??!?!")
        }
      }

  // Linking to FerrostarForegroundServiceManager & Core.
  var notificationBuilder: ForegroundNotificationBuilder? = null
    set(value) {
      field = value
      field?.channelId = CHANNEL_ID
      field?.openPendingIntent = createOpenAppIntent()
      field?.stopPendingIntent = createStopNavigationIntent()
    }

  override fun onBind(intent: Intent?): IBinder {
    return binder
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "onStartCommand")
    return START_STICKY
  }

  override fun onDestroy() {
    // TODO: Implement
    super.onDestroy()
  }

  fun start() {
    Log.d(TAG, "Starting service")

    createNotificationChannel(this)
    registerReceivers()
    val notification = buildNotification(null)
    // TODO: Anything else?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ServiceCompat.startForeground(
          this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    // TODO: Seems unnecessary
    //    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  fun stop() {
    notificationManager.cancel(NOTIFICATION_ID)
    this.unregisterReceiver(stopNavigationReceiver)
    stopSelf()
  }

  // Callback for NavigationState changes

  override fun onNavigationState(state: NavigationState) {
    val notification = buildNotification(state)
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  // Setup & support

  private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
          NotificationChannel(
              CHANNEL_ID,
              context.getString(R.string.notification_channel_description),
              NotificationManager.IMPORTANCE_LOW // TODO: Learn about Importance
              )
      notificationManager.createNotificationChannel(channel)
    }
    // TODO: Backwards compatibility?
  }

  // Notification builder

  private fun buildNotification(state: NavigationState?): Notification {
    val notificationBuilder =
        notificationBuilder ?: throw IllegalStateException("Notification builder is null")
    return notificationBuilder.build(tripState = state?.tripState)
  }

  // Intent builders for the notification.

  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  private fun registerReceivers() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      this.registerReceiver(
          stopNavigationReceiver,
          IntentFilter(STOP_NAVIGATION_INTENT),
          Context.RECEIVER_NOT_EXPORTED)
    } else {
      this.registerReceiver(stopNavigationReceiver, IntentFilter(STOP_NAVIGATION_INTENT))
    }
  }

  private fun createOpenAppIntent(): PendingIntent {
    val launchIntent =
        packageManager.getLaunchIntentForPackage(packageName)
            ?: throw IllegalStateException("Unable to find launch intent for package")
    launchIntent.setPackage(null)
    launchIntent.setAction(OPEN_NAVIGATION_INTENT)

    return PendingIntent.getActivity(
        this, 0, this.packageManager.getLaunchIntentForPackage(this.packageName), INTENT_FLAGS)
  }

  private fun createStopNavigationIntent(): PendingIntent {
    return PendingIntent.getBroadcast(this, 0, Intent(STOP_NAVIGATION_INTENT), INTENT_FLAGS)
  }
}
