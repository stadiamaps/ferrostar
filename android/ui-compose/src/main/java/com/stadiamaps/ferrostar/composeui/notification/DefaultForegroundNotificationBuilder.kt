package com.stadiamaps.ferrostar.composeui.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import com.stadiamaps.ferrostar.composeui.R
import com.stadiamaps.ferrostar.core.extensions.currentStep
import com.stadiamaps.ferrostar.ui.formatters.DateTimeFormatter
import com.stadiamaps.ferrostar.ui.formatters.DistanceFormatter
import com.stadiamaps.ferrostar.ui.formatters.DurationFormatter
import com.stadiamaps.ferrostar.ui.formatters.EstimatedArrivalDateTimeFormatter
import com.stadiamaps.ferrostar.ui.formatters.LocalizedDistanceFormatter
import com.stadiamaps.ferrostar.ui.formatters.LocalizedDurationFormatter
import com.stadiamaps.ferrostar.core.extensions.estimatedArrivalTime
import com.stadiamaps.ferrostar.core.service.ForegroundNotificationBuilder
import com.stadiamaps.ferrostar.ui.shared.icons.ManeuverIcon
import kotlin.time.ExperimentalTime
import uniffi.ferrostar.DrivingSide
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
        val drivingSide = tripState.currentStep()?.drivingSide ?: DrivingSide.RIGHT
        tripState.visualInstruction?.let {
          val contentView = getLayoutWith(tripProgress, it, drivingSide)
          val expandedView = getLayoutWith(tripProgress, it, drivingSide, expanded = true)
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

  @OptIn(ExperimentalTime::class)
  @SuppressLint("DiscouragedApi")
  private fun getLayoutWith(
      tripProgress: TripProgress,
      visualInstruction: VisualInstruction,
      drivingSide: DrivingSide,
      expanded: Boolean = false
  ): RemoteViews {
    val remoteViews =
        if (expanded) {
          RemoteViews(context.packageName, R.layout.expanded_navigation_notification)
        } else {
          RemoteViews(context.packageName, R.layout.navigation_notification)
        }

    val maneuverType = visualInstruction.primaryContent.maneuverType
    val maneuverModifier = visualInstruction.primaryContent.maneuverModifier
    val maneuverIcon = if (maneuverType != null && maneuverModifier != null) {
      ManeuverIcon(context, maneuverType, maneuverModifier, drivingSide)
    } else {
      null
    }

    maneuverIcon?.resourceId?.let {
      remoteViews.setImageViewResource(R.id.instruction_image, it)
    }

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
