package com.stadiamaps.ferrostar.composeui

import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.util.LocaleData
import android.icu.util.LocaleData.MeasurementSystem
import android.icu.util.MeasureUnit
import android.icu.util.ULocale

private const val METERS_PER_MILE = 1609.344
private const val FEET_PER_METER = 3.28084
private const val YARDS_PER_METER = 1.093613

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
class LocalizedDistanceFormatter(
    var localeOverride: ULocale? = null,
    var measurementSystemOverride: MeasurementSystem? = null
) : DistanceFormatter {
  override fun format(distanceInMeters: Double): String {
    val locale = localeOverride ?: ULocale.getDefault(ULocale.Category.FORMAT)
    val measurementSystem =
        measurementSystemOverride ?: LocaleData.getMeasurementSystem(locale) ?: MeasurementSystem.SI
    val unit: MeasureUnit
    val distance: Double
    val precision: Precision

    when (measurementSystem) {
      MeasurementSystem.US -> {
        if (distanceInMeters > 289) {
          // For longer distances (as we approach 1000 feet), use miles
          // (289m is just under 950ft, at which point we'd round up to 1,000)
          unit = MeasureUnit.MILE

          val distanceInMiles = distanceInMeters / METERS_PER_MILE
          distance = distanceInMiles
          precision =
              if (distanceInMiles > 10) {
                Precision.integer()
              } else {
                Precision.maxFraction(1)
              }
        } else {
          unit = MeasureUnit.FOOT
          precision = Precision.integer()

          val distanceInFeet = distanceInMeters * FEET_PER_METER
          distance =
              if (distanceInFeet < 50) {
                // Less than 50 feet, round to the nearest 5ft
                distanceInFeet.roundToNearest(5)
              } else if (distanceInFeet < 100) {
                // Between 50ft and 100ft, round to the nearest 10ft
                distanceInFeet.roundToNearest(10)
              } else if (distanceInFeet < 500) {
                // Between 100ft and 500ft, round to the nearest 50ft
                distanceInFeet.roundToNearest(50)
              } else {
                // Above 500 ft switches to 100ft
                distanceInFeet.roundToNearest(100)
              }
        }
      }
      MeasurementSystem.UK -> {
        if (distanceInMeters > 300) {
          // Use miles for longer distances (300m is around 0.2mi)
          unit = MeasureUnit.MILE

          val distanceInMiles = distanceInMeters / METERS_PER_MILE
          distance = distanceInMiles
          precision =
              if (distanceInMiles > 10) {
                Precision.integer()
              } else {
                Precision.maxFraction(1)
              }
        } else {
          unit = MeasureUnit.YARD
          precision = Precision.integer()

          val distanceInYards = distanceInMeters * YARDS_PER_METER

          distance =
              if (distanceInYards < 10) {
                // Less than 10 yards, round to the nearest 5
                distanceInYards.roundToNearest(5)
              } else {
                // Otherwise, round to the nearest 10
                distanceInYards.roundToNearest(10)
              }
        }
      }
      // NOTE: There is a rather annoying design flaw in the Android ICU API (or Kotlin itself,
      // depending on your point of view);
      // MeasurementSystem isn't a proper enum, nor is it a sealed class which allows for Kotlin to
      // prove exhaustive match.
      // Thus, we are forced to use `else` here and won't get a compiler error if a new variant is
      // introduced.
      else -> {
        if (distanceInMeters > 1_000) {
          // Longer distances: use km
          unit = MeasureUnit.KILOMETER
          distance = distanceInMeters / 1_000
          precision =
              if (distanceInMeters > 10_000) {
                // For distances > 10km, display in km and round to the nearest km.
                Precision.integer()
              } else {
                // Between 1km and 10km, display in km but increase resolution to 0.1km
                Precision.maxFraction(1)
              }
        } else {
          // Longer distances: use m
          unit = MeasureUnit.METER
          distance =
              if (distanceInMeters > 100) {
                // Round to nearest 100 meters
                distanceInMeters.roundToNearest(100)
              } else if (distanceInMeters > 10) {
                // Round to nearest 10 meters between 10m and 100m
                distanceInMeters.roundToNearest(10)
              } else {
                // Otherwise, round to the nearest 5
                distanceInMeters.roundToNearest(5)
              }
          precision = Precision.integer()
        }
      }
    }

    val formatter = NumberFormatter.withLocale(locale).precision(precision).unit(unit)

    return formatter.format(distance).toString()
  }
}

internal fun Double.roundToNearest(wholeNumber: Int): Double {
  return Math.round(this / wholeNumber).toDouble() * wholeNumber
}
