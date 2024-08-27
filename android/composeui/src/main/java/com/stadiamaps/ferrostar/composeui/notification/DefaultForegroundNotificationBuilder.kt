package com.stadiamaps.ferrostar.composeui.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import com.stadiamaps.ferrostar.composeui.R
import com.stadiamaps.ferrostar.composeui.formatting.DateTimeFormatter
import com.stadiamaps.ferrostar.composeui.formatting.DistanceFormatter
import com.stadiamaps.ferrostar.composeui.formatting.DurationFormatter
import com.stadiamaps.ferrostar.composeui.formatting.EstimatedArrivalDateTimeFormatter
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDistanceFormatter
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDurationFormatter
import com.stadiamaps.ferrostar.composeui.views.maneuver.maneuverIcon
import com.stadiamaps.ferrostar.core.extensions.estimatedArrivalTime
import com.stadiamaps.ferrostar.core.service.ForegroundNotificationBuilder
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.TripState
import uniffi.ferrostar.VisualInstruction

class DefaultForegroundNotificationBuilder(
    context: Context,
    private var estimatedArrivalFormatter: DateTimeFormatter = EstimatedArrivalDateTimeFormatter(),
    private var distanceFormatter: DistanceFormatter = LocalizedDistanceFormatter(),
    private var durationFormatter: DurationFormatter = LocalizedDurationFormatter(),
) : ForegroundNotificationBuilder(context) {

  override fun build(tripState: TripState?): Notification {
    if (channelId == null) {
      throw IllegalStateException("channelId must be set before building the notification.")
    }

    // Generate the notification builder. Note that channelId is set on newer versions of Android.
    // The channel is used to associate the notification in settings.
    val builder: Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          Notification.Builder(context, channelId)
        } else {
          Notification.Builder(context)
        }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // Colorize the notification w/ the background color.
      builder.setColorized(true).setColor(context.getColor(R.color.background_color))
    }

    // Set the notification's icon to a simple user location icon (Material's navigation icon).
    builder.setSmallIcon(R.drawable.notification_icon)

    // Build the notification's content based on the trip state.
    // When navigating, show the visual instruction, icon and formatted progress items.
    // When complete, show the arrival title and description.
    // Otherwise, show the preparing title (this typically only happens on launch).
    when (tripState) {
      is TripState.Navigating -> {
        val tripProgress = tripState.progress
        tripState.visualInstruction?.let {
          val contentView = getLayoutWith(tripProgress, it)
          val expandedView = getLayoutWith(tripProgress, it, expanded = true)
          expandedView.setOnClickPendingIntent(R.id.stop_navigation_button, stopPendingIntent)

          builder.setCustomContentView(contentView)
          builder.setCustomBigContentView(expandedView)
        }
      }
      is TripState.Complete -> {
        builder.setContentTitle(context.getString(R.string.arrived_title))
        builder.setContentText(context.getString(R.string.arrived_description))
      }
      else -> {
        builder.setContentTitle(context.getString(R.string.preparing))
      }
    }

    return builder
        .setOngoing(true)
        .setContentIntent(openPendingIntent)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .build()
  }

  @SuppressLint("DiscouragedApi")
  private fun getLayoutWith(
      tripProgress: TripProgress,
      visualInstruction: VisualInstruction,
      expanded: Boolean = false
  ): RemoteViews {
    val remoteViews =
        if (expanded) {
          RemoteViews(context.packageName, R.layout.expanded_navigation_notification)
        } else {
          RemoteViews(context.packageName, R.layout.navigation_notification)
        }

    val instructionImage = visualInstruction.primaryContent.maneuverIcon
    remoteViews.setImageViewResource(
        R.id.instruction_image,
        context.resources.getIdentifier(instructionImage, "drawable", context.packageName))

    // Set the text
    remoteViews.setTextViewText(
        R.id.estimated_arrival_time,
        estimatedArrivalFormatter.format(tripProgress.estimatedArrivalTime()))
    remoteViews.setTextViewText(
        R.id.duration_remaining, durationFormatter.format(tripProgress.durationRemaining))
    remoteViews.setTextViewText(
        R.id.distance_remaining, distanceFormatter.format(tripProgress.distanceRemaining))
    remoteViews.setTextViewText(R.id.instruction, visualInstruction.primaryContent.text)

    return remoteViews
  }
}
