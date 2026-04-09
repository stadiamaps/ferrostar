package com.stadiamaps.ferrostar.car.app.navigation

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stadiamaps.ferrostar.car.app.R
import uniffi.ferrostar.SpokenInstruction

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
 * @param contentIntent Optional [PendingIntent] fired when the user taps the HUN or rail widget,
 *   typically used to bring the car app back to the foreground.
 */
class TurnByTurnNotificationManager(
    private val context: Context,
    private val channelId: String = DEFAULT_CHANNEL_ID,
    private val notificationId: Int = DEFAULT_NOTIFICATION_ID,
    @DrawableRes private val smallIconRes: Int,
    private val contentIntent: PendingIntent? = null
) {

  private val notificationManager = NotificationManagerCompat.from(context)

  init {
    val channel =
        NotificationChannel(
            channelId, "Navigation",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = context.getString(R.string.notification_description)
        }
    notificationManager.createNotificationChannel(channel)
  }

  /** Posts or updates the turn-by-turn notification with the given [instruction]. */
  @SuppressLint("MissingPermission") // Caller is responsible for POST_NOTIFICATIONS permission.
  fun update(instruction: SpokenInstruction) {
    val notification =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(instruction.text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .extend(
                CarAppExtender.Builder()
                    .setContentTitle(instruction.text)
                    .apply {
                      contentIntent?.let {
                        setContentIntent(it)
                      }
                    }
                    .setImportance(NotificationManager.IMPORTANCE_HIGH)
                    .build()
            )
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
