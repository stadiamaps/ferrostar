package com.stadiamaps.ferrostar.core.service

import android.app.Notification
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.stadiamaps.ferrostar.core.R
import com.stadiamaps.ferrostar.core.extensions.estimatedArrivalTime
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.VisualInstruction

@RequiresApi(Build.VERSION_CODES.O)
class ForegroundNotification(
  val context: Context,
) {

  companion object {
    const val CHANNEL_ID = "navigation"
  }

  fun build(visualInstruction: VisualInstruction? = null): Notification {
    return Notification.Builder(context, CHANNEL_ID)
      .setContentTitle("Ferrostar")
      .setContentText(visualInstruction?.primaryContent?.text ?: "Loading directions...")
      .setColorized(true)
      .setColor(Color.CYAN)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
//      .setContentIntent(pendingIntent)
      .build()
  }

  fun getLayoutWith(tripProgress: TripProgress, visualInstruction: VisualInstruction): RemoteViews {
    val packageName = context.packageName
    val remoteViews = RemoteViews(packageName, R.layout.navigation_notification)

    remoteViews.setTextViewText(R.id.estimated_arrival_time, tripProgress.estimatedArrivalTime())
    remoteViews.setTextViewText(R.id.duration_remaining, "Ferrostar")
    remoteViews.setTextViewText(R.id.distance_remaining, "Ferrostar")
    remoteViews.setTextViewText(R.id.instruction, visualInstruction.primaryContent.text)

    return remoteViews
  }
}