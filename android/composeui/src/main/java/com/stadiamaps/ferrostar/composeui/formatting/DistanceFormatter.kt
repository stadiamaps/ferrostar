package com.stadiamaps.ferrostar.composeui.formatting

import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.text.MeasureFormat
import android.icu.text.NumberFormat
import android.icu.util.LocaleData
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import android.os.Build

private const val METERS_PER_MILE = 1609.344
private const val FEET_PER_METER = 3.28084
private const val YARDS_PER_METER = 1.093613

/** Measurement system model that backports Java APIs was not available before API 28. */
enum class DistanceMeasurementSystem {
  /** Metric system; used by most of the world. */
  SI,

  /** The US version of the imperial system which uses feet and miles as distance units. */
  IMPERIAL,

  /** The UK version of the imperial system which uses yards and miles as distance units. */
  IMPERIAL_WITH_YARDS
}

/**
 * Describes how much decimal precision should be shown for display purposes.
 *
 * This is basically a minimal backport of the Precision API in Android 30.
 */
private enum class DecimalPrecision {
  /** Rounds to the nearest integer. */
  NEAREST_INTEGER,
  /** Rounds to the nearest tenth (0.1). */
  NEAREST_TENTH,
}

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
    var distanceMeasurementSystemOverride: DistanceMeasurementSystem? = null
) : DistanceFormatter {
  override fun format(distanceInMeters: Double): String {
    val locale = localeOverride ?: ULocale.getDefault(ULocale.Category.FORMAT)
    val measurementSystem = distanceMeasurementSystemOverride ?: getMeasurementSystem(locale)
    val unit: MeasureUnit
    val distance: Double
    val precision: DecimalPrecision

    when (measurementSystem) {
      DistanceMeasurementSystem.IMPERIAL -> {
        if (distanceInMeters > 289) {
          // For longer distances (as we approach 1000 feet), use miles
          // (289m is just under 950ft, at which point we'd round up to 1,000)
          unit = MeasureUnit.MILE

          val distanceInMiles = distanceInMeters / METERS_PER_MILE
          distance = distanceInMiles
          precision =
              if (distanceInMiles > 10) {
                DecimalPrecision.NEAREST_INTEGER
              } else {
                DecimalPrecision.NEAREST_TENTH
              }
        } else {
          unit = MeasureUnit.FOOT
          precision = DecimalPrecision.NEAREST_INTEGER

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
      DistanceMeasurementSystem.IMPERIAL_WITH_YARDS -> {
        if (distanceInMeters > 300) {
          // Use miles for longer distances (300m is around 0.2mi)
          unit = MeasureUnit.MILE

          val distanceInMiles = distanceInMeters / METERS_PER_MILE
          distance = distanceInMiles
          precision =
              if (distanceInMiles > 10) {
                DecimalPrecision.NEAREST_INTEGER
              } else {
                DecimalPrecision.NEAREST_TENTH
              }
        } else {
          unit = MeasureUnit.YARD
          precision = DecimalPrecision.NEAREST_INTEGER

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
      DistanceMeasurementSystem.SI -> {
        if (distanceInMeters > 1_000) {
          // Longer distances: use km
          unit = MeasureUnit.KILOMETER
          distance = distanceInMeters / 1_000
          precision =
              if (distanceInMeters > 10_000) {
                // For distances > 10km, display in km and round to the nearest km.
                DecimalPrecision.NEAREST_INTEGER
              } else {
                // Between 1km and 10km, display in km but increase resolution to 0.1km
                DecimalPrecision.NEAREST_TENTH
              }
        } else {
          // Shorter distances: use m
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
          precision = DecimalPrecision.NEAREST_INTEGER
        }
      }
    }

    return formatDistance(distance, unit, locale, precision)
  }
}

internal fun Double.roundToNearest(wholeNumber: Int): Double {
  return Math.round(this / wholeNumber).toDouble() * wholeNumber
}

internal fun getMeasurementSystem(locale: ULocale): DistanceMeasurementSystem =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      when (LocaleData.getMeasurementSystem(locale) ?: LocaleData.MeasurementSystem.SI) {
        LocaleData.MeasurementSystem.US -> DistanceMeasurementSystem.IMPERIAL
        LocaleData.MeasurementSystem.UK -> DistanceMeasurementSystem.IMPERIAL_WITH_YARDS
        // Ideally we'd match exhaustively, but the underlying Java API and the design of Kotlin
        // make this impossible to do, so we're stuck with an else.
        else -> DistanceMeasurementSystem.SI
      }
    } else {
      when (locale.isO3Country) {
        "USA",
        "LBR",
        "MMR" -> DistanceMeasurementSystem.IMPERIAL
        "GBR" -> DistanceMeasurementSystem.IMPERIAL_WITH_YARDS
        else -> DistanceMeasurementSystem.SI
      }
    }

private fun formatDistance(
    distance: Double,
    unit: MeasureUnit,
    locale: ULocale,
    decimalPrecision: DecimalPrecision
): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val formatter =
          NumberFormatter.withLocale(locale)
              .precision(
                  when (decimalPrecision) {
                    DecimalPrecision.NEAREST_INTEGER -> Precision.integer()
                    DecimalPrecision.NEAREST_TENTH -> Precision.maxFraction(1)
                  })
              .unit(unit)

      formatter.format(distance).toString()
    } else {
      val numberFormat = NumberFormat.getInstance(locale)
      numberFormat.maximumFractionDigits =
          when (decimalPrecision) {
            DecimalPrecision.NEAREST_INTEGER -> 0
            DecimalPrecision.NEAREST_TENTH -> 1
          }
      val formatter =
          MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT, numberFormat)
      val measure = Measure(distance, unit)

      formatter.format(measure)
    }
