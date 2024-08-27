package com.stadiamaps.ferrostar.core.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.lang.ref.WeakReference
import uniffi.ferrostar.TripState

/**
 * A builder for creating a foreground notification for the Ferrostar navigation service.
 *
 * See [DefaultForegroundNotificationBuilder] in the composeui module for an example/default
 * implementation.
 */
abstract class ForegroundNotificationBuilder(context: Context) {

  var channelId: String? = null
  val openPendingIntent: PendingIntent by lazy { createOpenAppIntent() }
  val stopPendingIntent: PendingIntent by lazy { createStopNavigationIntent() }

  companion object {
    const val INTENT_FLAGS: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    const val STOP_NAVIGATION_INTENT = "com.ferrostar.intent.action.STOP_NAVIGATION"
    const val OPEN_NAVIGATION_INTENT = "com.ferrostar.intent.action.OPEN_NAVIGATION"
  }

  private val weakContext: WeakReference<Context> = WeakReference(context)
  val context: Context
    get() = weakContext.get() ?: throw IllegalStateException("Context is null")

  /**
   * This pending intent will open the host app when the notification is clicked. It is used to
   * launch the in-progress navigation UI.
   *
   * @return A pending intent that will open the host app when clicked.
   */
  private fun createOpenAppIntent(): PendingIntent {
    val launchIntent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: throw IllegalStateException("Unable to find launch intent for package")
    launchIntent.setPackage(null)
    launchIntent.setAction(OPEN_NAVIGATION_INTENT)

    return PendingIntent.getActivity(
        context,
        0,
        context.packageManager.getLaunchIntentForPackage(context.packageName),
        INTENT_FLAGS)
  }

  /**
   * This pending intent will stop the navigation service when the notification is clicked. It is
   * used to stop the navigation service and dismiss the notification.
   *
   * @return A pending intent that will stop the navigation service when clicked.
   */
  private fun createStopNavigationIntent(): PendingIntent {
    val intent = Intent(STOP_NAVIGATION_INTENT)
    return PendingIntent.getBroadcast(context, 0, intent, INTENT_FLAGS)
  }

  abstract fun build(tripState: TripState?): Notification
}
