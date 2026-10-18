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
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.RouteStep
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

  private fun step(distance: Double, duration: Double) =
      RouteStep(
          geometry = listOf(),
          distance = distance,
          duration = duration,
          roadName = null,
          exits = listOf(),
          instruction = "Turn right",
          visualInstructions = listOf(),
          spokenInstructions = listOf(),
          annotations = null,
          incidents = listOf(),
          drivingSide = DrivingSide.RIGHT,
          roundaboutExitNumber = null,
      )

  private fun progress(distanceToNextManeuver: Double, distanceRemaining: Double = 10_000.0) =
      TripProgress(
          distanceToNextManeuver = distanceToNextManeuver,
          distanceRemaining = distanceRemaining,
          durationRemaining = 3_600.0,
      )

  @Test
  fun stepEstimateUsesTheDistanceToTheNextManeuverAndNotTheTripDistance() {
    val step = step(distance = 2_000.0, duration = 200.0)
    val progress = progress(distanceToNextManeuver = 1_770.0, distanceRemaining = 11_400.0)

    val estimate = progress.toCarStepTravelEstimate(step, Instant.fromEpochSeconds(0))

    assertEquals(1_770.0.toCarDistance(), estimate.remainingDistance)
    assertNotEquals(11_400.0.toCarDistance(), estimate.remainingDistance)
  }

  @Test
  fun stepEstimateRemainingTimeAndArrivalTimeFollowTheStepDuration() {
    val step = step(distance = 1_000.0, duration = 100.0)
    val start = Instant.fromEpochSeconds(1_720_289_000)

    val estimate = progress(250.0).toCarStepTravelEstimate(step, start)

    assertEquals(25L, estimate.remainingTimeSeconds)
    assertEquals(
        (1_720_289_000L + 25L) * 1_000L,
        estimate.arrivalTimeAtDestination?.timeSinceEpochMillis,
    )
  }
}
