package com.stadiamaps.ferrostar.car.app.template.models

import android.icu.util.MeasureUnit
import androidx.car.app.model.CarColor
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.TravelEstimate
import com.stadiamaps.ferrostar.core.extensions.estimatedArrivalTime
import com.stadiamaps.ferrostar.ui.formatters.LocalizedDistanceFormatter
import java.util.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.toInstant
import uniffi.ferrostar.RouteStep
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
  val unit =
      when (formatter.recommendedUnit(this)) {
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
  val arrivalMillis =
      estimatedArrivalTime()
          .toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
          .toEpochMilliseconds()

  val arrivalDateTimeWithZone = DateTimeWithZone.create(arrivalMillis, TimeZone.getDefault())

  return TravelEstimate.Builder(distanceRemaining.toCarDistance(), arrivalDateTimeWithZone)
      .setRemainingTimeSeconds(durationRemaining.toLong())
      .setRemainingTimeColor(CarColor.GREEN)
      .build()
}

/**
 * The estimated time, in seconds, until the next maneuver.
 *
 * The core exposes the distance to the next maneuver but not the time. This function scales
 * [RouteStep.duration] by the fraction of [RouteStep.distance] that is still ahead. It clamps the
 * result to the range `[0, currentStep.duration]`.
 *
 * Returns zero when the step has no positive finite distance or duration, or when the fraction is
 * not a number.
 */
fun TripProgress.durationToNextManeuver(currentStep: RouteStep): Double {
  val stepDistance = currentStep.distance
  val stepDuration = currentStep.duration
  // A negated comparison also rejects NaN. An infinite duration would overflow the arrival time.
  if (!(stepDistance > 0.0) || !(stepDuration > 0.0) || !stepDuration.isFinite()) {
    return 0.0
  }

  val remainingDuration = stepDuration * (distanceToNextManeuver / stepDistance)
  if (remainingDuration.isNaN()) {
    return 0.0
  }

  return remainingDuration.coerceIn(0.0, stepDuration)
}

/**
 * Builds a Car App Library [TravelEstimate] for the current step from this [TripProgress].
 *
 * The Car App Library shows the first step estimate on the instrument cluster and in the floating
 * navigation bar. This function therefore uses [TripProgress.distanceToNextManeuver] and not the
 * whole-trip [TripProgress.distanceRemaining]. The remaining time is [durationToNextManeuver]. The
 * arrival time is [fromDate] plus that remaining time.
 *
 * This mirrors `CPTravelEstimates.fromFerrostarForStep` on CarPlay.
 *
 * @param currentStep The step the user is on. It gives the time to the next maneuver.
 * @param fromDate The starting instant. Defaults to the current time. Tests can set a fixed value.
 */
@OptIn(ExperimentalTime::class)
fun TripProgress.toCarStepTravelEstimate(
    currentStep: RouteStep,
    fromDate: Instant = Clock.System.now(),
): TravelEstimate {
  val remainingSeconds = durationToNextManeuver(currentStep)
  val arrivalMillis = (fromDate + remainingSeconds.seconds).toEpochMilliseconds()
  val arrivalDateTimeWithZone = DateTimeWithZone.create(arrivalMillis, TimeZone.getDefault())

  return TravelEstimate.Builder(distanceToNextManeuver.toCarDistance(), arrivalDateTimeWithZone)
      .setRemainingTimeSeconds(remainingSeconds.toLong())
      .setRemainingTimeColor(CarColor.GREEN)
      .build()
}
