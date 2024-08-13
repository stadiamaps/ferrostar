package com.stadiamaps.ferrostar.core.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class ForegroundServiceManager(
  private val context: Context
) {

  companion object {
    val INTENT_FLAGS: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  }

  fun startService() {
    val intent = Intent(context, ForegroundService::class.java)
    context.startService(intent)
  }

  fun createOpenAppIntent(): PendingIntent {
    return PendingIntent.getActivity(
      context,
      0,
      context.packageManager.getLaunchIntentForPackage(context.packageName),
      INTENT_FLAGS
    )
  }

  fun createEndNavigationIntent(): PendingIntent {
    val intent = Intent(STOP_NAVIGATION_ACTION)
    return PendingIntent.getBroadcast(
      context,
      0,
      endNavigationBtn,
      INTENT_FLAGS
    );
  }
}