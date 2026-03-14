package com.stadiamaps.ferrostar.composeui.formatting

import android.icu.util.ULocale
import com.stadiamaps.ferrostar.core.measurement.DistanceMeasurementSystem

/**
 * A generic interface defining distance formatters.
 *
 * Regrettably, the Android standard libraries lack a reliable method of determining which unit to
 * use when displaying distances. This interface allows you to implement
 */
fun interface DistanceFormatter {
  /**
   * Formats a distance, given in meters, as human-readable (ex: rounded, localized, etc.) output.
   */
  fun format(distanceInMeters: Double): String
}

/**
 * A distance formatter that attempts to handle formatting in a sane way for the locale.
 *
 * The default locale will be used if none is specified (and the default locale will be checked
 * EVERY TIME that `format` is called!).
 *
 * In addition to overriding the locale, you can override the measurement system to handle cases
 * like a user with a US phone locale preferring metric measurements.
 */
@Deprecated(message = "Use ui.support.LocalizedDistanceFormatter")
class LocalizedDistanceFormatter(
    var localeOverride: ULocale? = null,
    var distanceMeasurementSystemOverride: DistanceMeasurementSystem? = null
) : DistanceFormatter {

  private val formatter = com.stadiamaps.ferrostar.ui.support.formatter.LocalizedDistanceFormatter(
      localeOverride, distanceMeasurementSystemOverride
  )

  override fun format(distanceInMeters: Double): String =
    formatter.format(distanceInMeters)
}
