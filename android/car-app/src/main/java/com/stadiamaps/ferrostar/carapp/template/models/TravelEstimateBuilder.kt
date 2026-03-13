package com.stadiamaps.ferrostar.carapp.template.models

import android.icu.util.ULocale
import androidx.car.app.model.CarColor
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.TravelEstimate
import com.stadiamaps.ferrostar.core.measurement.DistanceMeasurementSystem
import com.stadiamaps.ferrostar.core.measurement.getMeasurementSystem
import java.time.ZonedDateTime
import java.util.TimeZone
import uniffi.ferrostar.TripProgress

private const val METERS_PER_MILE = 1609.344
private const val FEET_PER_METER = 3.28084
private const val YARDS_PER_METER = 1.093613

// Imperial: switch to miles above ~289m (just under 950ft, rounds to 1,000)
private const val IMPERIAL_LARGE_UNIT_THRESHOLD_METERS = 289.0

// UK imperial: switch to miles above 300m (~0.2mi)
private const val IMPERIAL_YARDS_LARGE_UNIT_THRESHOLD_METERS = 300.0

// SI: switch to km above 1000m
private const val SI_LARGE_UNIT_THRESHOLD_METERS = 1_000.0

/**
 * Converts a distance in meters to a Car App Library [Distance] using locale-appropriate units.
 *
 * Uses the same measurement system detection as [LocalizedDistanceFormatter]: SI locales get
 * meters/km, US locales get feet/miles, UK locales get yards/miles.
 */
fun Double.toCarDistance(locale: ULocale = ULocale.getDefault(ULocale.Category.FORMAT)): Distance {
  return when (getMeasurementSystem(locale)) {
    DistanceMeasurementSystem.IMPERIAL ->
        if (this > IMPERIAL_LARGE_UNIT_THRESHOLD_METERS) {
          Distance.create(this / METERS_PER_MILE, Distance.UNIT_MILES)
        } else {
          Distance.create(this * FEET_PER_METER, Distance.UNIT_FEET)
        }
    DistanceMeasurementSystem.IMPERIAL_WITH_YARDS ->
        if (this > IMPERIAL_YARDS_LARGE_UNIT_THRESHOLD_METERS) {
          Distance.create(this / METERS_PER_MILE, Distance.UNIT_MILES)
        } else {
          Distance.create(this * YARDS_PER_METER, Distance.UNIT_YARDS)
        }
    DistanceMeasurementSystem.SI ->
        if (this > SI_LARGE_UNIT_THRESHOLD_METERS) {
          Distance.create(this / 1_000.0, Distance.UNIT_KILOMETERS)
        } else {
          Distance.create(this, Distance.UNIT_METERS)
        }
  }
}

/**
 * Converts the distance to next maneuver to a Car App Library [Distance].
 *
 * Returns a zero-meter [Distance] if the receiver is null.
 */
fun TripProgress?.toCarDistanceToNextManeuver(
    locale: ULocale = ULocale.getDefault(ULocale.Category.FORMAT)
): Distance = (this?.distanceToNextManeuver ?: 0.0).toCarDistance(locale)

/**
 * Builds a Car App Library [TravelEstimate] from this [TripProgress].
 *
 * Computes the ETA by adding [TripProgress.durationRemaining] to the current system time.
 */
fun TripProgress.toCarTravelEstimate(
    locale: ULocale = ULocale.getDefault(ULocale.Category.FORMAT)
): TravelEstimate {
  val arrival = ZonedDateTime.now().plusSeconds(durationRemaining.toLong())
  val arrivalDateTimeWithZone =
      DateTimeWithZone.create(arrival.toInstant().toEpochMilli(), TimeZone.getDefault())

  return TravelEstimate.Builder(distanceRemaining.toCarDistance(locale), arrivalDateTimeWithZone)
      .setRemainingTimeSeconds(durationRemaining.toLong())
      .setRemainingTimeColor(CarColor.GREEN)
      .build()
}
