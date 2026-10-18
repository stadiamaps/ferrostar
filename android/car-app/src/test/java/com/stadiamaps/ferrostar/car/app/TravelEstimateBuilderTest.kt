package com.stadiamaps.ferrostar.car.app

import com.stadiamaps.ferrostar.car.app.template.models.durationToNextManeuver
import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.RouteStep
import uniffi.ferrostar.TripProgress

class TravelEstimateBuilderTest {

  private val tolerance = 1e-9

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
  fun `duration scales with the fraction of the step still ahead`() {
    val step = step(distance = 1_000.0, duration = 100.0)

    assertEquals(50.0, progress(500.0).durationToNextManeuver(step), tolerance)
    assertEquals(25.0, progress(250.0).durationToNextManeuver(step), tolerance)
  }

  @Test
  fun `duration is the full step duration at the start of the step`() {
    val step = step(distance = 1_000.0, duration = 100.0)

    assertEquals(100.0, progress(1_000.0).durationToNextManeuver(step), tolerance)
  }

  @Test
  fun `duration is zero at the maneuver`() {
    val step = step(distance = 1_000.0, duration = 100.0)

    assertEquals(0.0, progress(0.0).durationToNextManeuver(step), tolerance)
  }

  @Test
  fun `duration clamps to the step duration when the remaining distance exceeds the step`() {
    val step = step(distance = 1_000.0, duration = 100.0)

    assertEquals(100.0, progress(1_500.0).durationToNextManeuver(step), tolerance)
  }

  @Test
  fun `duration clamps to zero for a negative remaining distance`() {
    val step = step(distance = 1_000.0, duration = 100.0)

    assertEquals(0.0, progress(-5.0).durationToNextManeuver(step), tolerance)
  }

  @Test
  fun `duration is zero for a step with zero distance`() {
    val step = step(distance = 0.0, duration = 100.0)

    assertEquals(0.0, progress(0.0).durationToNextManeuver(step), tolerance)
    assertEquals(0.0, progress(500.0).durationToNextManeuver(step), tolerance)
  }

  @Test
  fun `duration is zero for a step with zero or negative duration`() {
    assertEquals(
        0.0,
        progress(500.0).durationToNextManeuver(step(distance = 1_000.0, duration = 0.0)),
        tolerance,
    )
    assertEquals(
        0.0,
        progress(500.0).durationToNextManeuver(step(distance = 1_000.0, duration = -10.0)),
        tolerance,
    )
  }

  @Test
  fun `duration is zero for non-finite inputs`() {
    val step = step(distance = 1_000.0, duration = 100.0)

    assertEquals(0.0, progress(Double.NaN).durationToNextManeuver(step), tolerance)
    assertEquals(
        0.0,
        progress(500.0).durationToNextManeuver(step(distance = Double.NaN, duration = 100.0)),
        tolerance,
    )
    assertEquals(
        0.0,
        progress(500.0)
            .durationToNextManeuver(step(distance = 1_000.0, duration = Double.POSITIVE_INFINITY)),
        tolerance,
    )
  }

  @Test
  fun `duration clamps to the step duration for an infinite remaining distance`() {
    val step = step(distance = 1_000.0, duration = 100.0)

    assertEquals(
        100.0,
        progress(Double.POSITIVE_INFINITY).durationToNextManeuver(step),
        tolerance,
    )
  }

  @Test
  fun `duration ignores the whole-trip distance remaining`() {
    val step = step(distance = 1_000.0, duration = 100.0)

    assertEquals(
        progress(500.0, distanceRemaining = 500.0).durationToNextManeuver(step),
        progress(500.0, distanceRemaining = 50_000.0).durationToNextManeuver(step),
        tolerance,
    )
  }
}
