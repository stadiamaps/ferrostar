package com.stadiamaps.ferrostar.car.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stadiamaps.ferrostar.car.app.template.models.toCarDistance
import com.stadiamaps.ferrostar.car.app.template.models.toCarStepTravelEstimate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import uniffi.ferrostar.TripProgress

/**
 * Tests for [toCarStepTravelEstimate].
 *
 * These run on a device because [toCarDistance] uses the Android ICU locale data, which the JVM
 * unit test stubs do not provide.
 */
@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class TravelEstimateBuilderInstrumentedTest {

  private val start = Instant.fromEpochSeconds(1_720_289_000)

  private fun progress(
      distanceToNextManeuver: Double = 1_770.0,
      durationToNextManeuver: Double = 25.0,
  ) =
      TripProgress(
          distanceToNextManeuver = distanceToNextManeuver,
          distanceRemaining = 11_400.0,
          durationRemaining = 3_600.0,
          durationToNextManeuver = durationToNextManeuver,
      )

  @Test
  fun stepEstimateUsesTheDistanceToTheNextManeuverAndNotTheTripDistance() {
    val estimate = progress().toCarStepTravelEstimate(start)

    assertEquals(1_770.0.toCarDistance(), estimate.remainingDistance)
    assertNotEquals(11_400.0.toCarDistance(), estimate.remainingDistance)
  }

  @Test
  fun stepEstimateUsesTheDurationToTheNextManeuverAndNotTheTripDuration() {
    val estimate = progress(durationToNextManeuver = 25.0).toCarStepTravelEstimate(start)

    assertEquals(25L, estimate.remainingTimeSeconds)
    assertEquals(
        (1_720_289_000L + 25L) * 1_000L,
        estimate.arrivalTimeAtDestination?.timeSinceEpochMillis,
    )
  }

  @Test
  fun stepEstimateUsesZeroSecondsForInvalidDurations() {
    for (duration in listOf(-5.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
      val estimate = progress(durationToNextManeuver = duration).toCarStepTravelEstimate(start)

      assertEquals("duration $duration", 0L, estimate.remainingTimeSeconds)
      assertEquals(
          "duration $duration",
          1_720_289_000L * 1_000L,
          estimate.arrivalTimeAtDestination?.timeSinceEpochMillis,
      )
    }
  }
}
