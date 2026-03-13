package com.stadiamaps.ferrostar.carapp.navigation

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uniffi.ferrostar.VisualInstruction

/**
 * Posts and updates a persistent heads-up notification for turn-by-turn guidance on Android Auto.
 *
 * This satisfies NF-3 compliance: when the user switches away from the navigation app, the
 * notification keeps them informed of the next maneuver.
 *
 * @param context Application context.
 * @param channelId Notification channel ID. Defaults to "ferrostar_navigation".
 * @param notificationId Notification ID. Defaults to 502.
 * @param smallIconRes Drawable resource ID for the notification's small icon.
 */
class TurnByTurnNotificationManager(
    private val context: Context,
    private val channelId: String = DEFAULT_CHANNEL_ID,
    private val notificationId: Int = DEFAULT_NOTIFICATION_ID,
    @DrawableRes private val smallIconRes: Int
) {

  private val notificationManager = NotificationManagerCompat.from(context)

  init {
    val channel =
        NotificationChannel(channelId, "Navigation", NotificationManager.IMPORTANCE_HIGH).apply {
          description = "Turn-by-turn navigation directions"
        }
    notificationManager.createNotificationChannel(channel)
  }

  /** Posts or updates the turn-by-turn notification with the given [instruction]. */
  @SuppressLint("MissingPermission") // Caller is responsible for POST_NOTIFICATIONS permission.
  fun update(instruction: VisualInstruction) {
    val notification =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(instruction.primaryContent.text)
            .setContentText(instruction.secondaryContent?.text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .extend(
                CarAppExtender.Builder().setImportance(NotificationManager.IMPORTANCE_HIGH).build())
            .build()

    try {
      notificationManager.notify(notificationId, notification)
    } catch (_: SecurityException) {
      // POST_NOTIFICATIONS permission not granted; nothing we can do here.
    }
  }

  /** Cancels the turn-by-turn notification. */
  fun clear() {
    notificationManager.cancel(notificationId)
  }

  companion object {
    const val DEFAULT_CHANNEL_ID = "ferrostar_navigation"
    const val DEFAULT_NOTIFICATION_ID = 502
  }
}
