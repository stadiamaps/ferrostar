package com.stadiamaps.ferrostar.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.stadiamaps.ferrostar.core.NavigationState
import com.stadiamaps.ferrostar.core.NavigationStateObserver
import com.stadiamaps.ferrostar.core.R

/**
 * A foreground service for the Ferrostar navigation service. This service is responsible for
 * displaying a notification while the navigation service is running.
 *
 * It runs in it's own context and is used to ensure location and navigation services are running
 * even when the app is in the background.
 */
class FerrostarForegroundService : Service(), NavigationStateObserver {

  companion object {
    const val NOTIFICATION_ID = 501
  }

  inner class LocalBinder : Binder() {
    val service: FerrostarForegroundService
      get() = this@FerrostarForegroundService
  }

  private val binder = LocalBinder()

  private val notificationManager: NotificationManager by lazy {
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  // Linking to FerrostarForegroundServiceManager & Core.
  var notificationBuilder: ForegroundNotificationBuilder? = null

  override fun onBind(intent: Intent?): IBinder {
    return binder
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  fun start() {
    createNotificationChannel()
    val notification = buildNotification(null)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ServiceCompat.startForeground(
          this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  fun stop() {
    notificationManager.cancel(NOTIFICATION_ID)
    stopSelf()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channelId =
          notificationBuilder?.channelId
              ?: throw IllegalStateException("Notification channel ID is null")

      val channel =
          NotificationChannel(
              channelId,
              this.getString(R.string.notification_channel_description),
              NotificationManager.IMPORTANCE_LOW // TODO: Learn about Importance
              )
      notificationManager.createNotificationChannel(channel)
    }
  }

  // Callback for NavigationState changes

  override fun onNavigationStateUpdated(state: NavigationState) {
    val notification = buildNotification(state)
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  // Notification builder

  private fun buildNotification(state: NavigationState?): Notification {
    val notificationBuilder =
        notificationBuilder ?: throw IllegalStateException("Notification builder is null")
    return notificationBuilder.build(tripState = state?.tripState)
  }
}
