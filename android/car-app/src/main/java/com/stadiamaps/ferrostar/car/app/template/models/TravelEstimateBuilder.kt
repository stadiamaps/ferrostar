package com.stadiamaps.ferrostar.car.app.template.models

import android.icu.util.MeasureUnit
import androidx.car.app.model.CarColor
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.TravelEstimate
import com.stadiamaps.ferrostar.core.extensions.estimatedArrivalTime
import com.stadiamaps.ferrostar.ui.formatters.LocalizedDistanceFormatter
import java.util.TimeZone
import kotlinx.datetime.toInstant
import uniffi.ferrostar.TripProgress


/**
 * Converts a distance in meters to a Car App Library [Distance] using locale-appropriate units.
 *
 * Uses the same measurement system detection as [LocalizedDistanceFormatter]: SI locales get
 * meters/km, US locales get feet/miles, UK locales get yards/miles.
 */
fun Double.toCarDistance(): Distance {
  val formatter = LocalizedDistanceFormatter()
  val roundedDistance = formatter.roundedDistanceForUnit(this)
  val unit = when (formatter.recommendedUnit(this)) {
    MeasureUnit.MILE -> Distance.UNIT_MILES
    MeasureUnit.YARD -> Distance.UNIT_YARDS
    MeasureUnit.FOOT -> Distance.UNIT_FEET
    MeasureUnit.KILOMETER -> Distance.UNIT_KILOMETERS
    MeasureUnit.METER -> Distance.UNIT_METERS
    else -> Distance.UNIT_METERS
  }
  return Distance.create(roundedDistance, unit)
}

/**
 * Converts the distance to next maneuver to a Car App Library [Distance].
 *
 * Returns a zero-meter [Distance] if the receiver is null.
 */
fun TripProgress?.toCarDistanceToNextManeuver(): Distance =
    (this?.distanceToNextManeuver ?: 0.0).toCarDistance()

/**
 * Builds a Car App Library [TravelEstimate] from this [TripProgress].
 *
 * Computes the ETA by adding [TripProgress.durationRemaining] to the current system time.
 */
fun TripProgress.toCarTravelEstimate(): TravelEstimate {
  val arrivalMillis = estimatedArrivalTime()
      .toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
      .toEpochMilliseconds()

  val arrivalDateTimeWithZone =
      DateTimeWithZone.create(arrivalMillis, TimeZone.getDefault())

  return TravelEstimate.Builder(
      distanceRemaining.toCarDistance(),
      arrivalDateTimeWithZone
  )
      .setRemainingTimeSeconds(durationRemaining.toLong())
      .setRemainingTimeColor(CarColor.GREEN)
      .build()
}
