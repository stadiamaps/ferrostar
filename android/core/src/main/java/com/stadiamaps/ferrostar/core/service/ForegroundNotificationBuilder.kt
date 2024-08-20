package com.stadiamaps.ferrostar.core.service

import android.app.Notification
import android.app.PendingIntent
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.TripState
import uniffi.ferrostar.VisualInstruction

/**
 * A builder for creating a foreground notification for the Ferrostar navigation service.
 *
 * See DefaultForegroundNotificationBuilder in the composeui module for an example/default implementation.
 */
interface ForegroundNotificationBuilder {
  var channelId: String?
  var openPendingIntent: PendingIntent?
  var stopPendingIntent: PendingIntent?

  fun build(tripState: TripState?): Notification
}
