package com.stadiamaps.ferrostar.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat

@RequiresApi(Build.VERSION_CODES.O)
class ForegroundService: Service() {

  companion object {
    const val TAG = "FerrostarForegroundService"
    const val CHANNEL_ID = "ferrostar_navigation"
    const val NOTIFICATION_ID = 501
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "onStartCommand")

    createChannel(this)
    start()

    return START_STICKY
  }

  private fun createChannel(context: Context) {
    val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

    // create the notification channel
    val channel = NotificationChannel(
      CHANNEL_ID,
      "ferrostar_channel",
      NotificationManager.IMPORTANCE_DEFAULT
    )

    notificationManager.createNotificationChannel(channel)
  }

  private fun start() {
    val notification = FerrostarNotification(this).build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ServiceCompat.startForeground(
        this,
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }
}