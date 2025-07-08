package com.stadiamaps.ferrostar.core

import com.stadiamaps.ferrostar.core.extensions.estimatedArrivalTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.ferrostar.TripProgress

@OptIn(ExperimentalTime::class)
class TripProgressTest {

  private val timeZone = TimeZone.UTC
  private val startInstant = Instant.fromEpochSeconds(1720289000)

  @Test
  fun testEstimatedArrivalTime() {
    val tripProgress =
        TripProgress(
            distanceToNextManeuver = 1.0, distanceRemaining = 1.0, durationRemaining = 3600.0)

    val expected = Instant.fromEpochSeconds(1720292600).toLocalDateTime(timeZone)

    assertEquals(expected, tripProgress.estimatedArrivalTime(startInstant, timeZone))
  }
}
