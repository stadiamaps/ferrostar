package com.stadiamaps.ferrostar.carapp.models

import android.icu.util.ULocale
import androidx.car.app.model.Distance
import com.stadiamaps.ferrostar.carapp.template.models.toCarDistance
import com.stadiamaps.ferrostar.carapp.template.models.toCarDistanceToNextManeuver
import com.stadiamaps.ferrostar.carapp.template.models.toCarTravelEstimate
import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.ferrostar.TripProgress

class TravelEstimateBuilderTest {

  private val siLocale = ULocale("fr_FR")
  private val usLocale = ULocale("en_US")
  private val ukLocale = ULocale("en_GB")

  // toCarDistance

  @Test
  fun `SI short distance stays in meters`() {
    val d = 500.0.toCarDistance(siLocale)
    assertEquals(500.0, d.displayDistance, 0.001)
    assertEquals(Distance.UNIT_METERS, d.displayUnit)
  }

  @Test
  fun `SI long distance converts to kilometers`() {
    val d = 5000.0.toCarDistance(siLocale)
    assertEquals(5.0, d.displayDistance, 0.001)
    assertEquals(Distance.UNIT_KILOMETERS, d.displayUnit)
  }

  @Test
  fun `US short distance converts to feet`() {
    val d = 100.0.toCarDistance(usLocale)
    assertEquals(328.084, d.displayDistance, 0.001)
    assertEquals(Distance.UNIT_FEET, d.displayUnit)
  }

  @Test
  fun `US long distance converts to miles`() {
    val d = 1600.0.toCarDistance(usLocale)
    assertEquals(1600.0 / 1609.344, d.displayDistance, 0.001)
    assertEquals(Distance.UNIT_MILES, d.displayUnit)
  }

  @Test
  fun `UK short distance converts to yards`() {
    val d = 200.0.toCarDistance(ukLocale)
    assertEquals(200.0 * 1.093613, d.displayDistance, 0.001)
    assertEquals(Distance.UNIT_YARDS, d.displayUnit)
  }

  @Test
  fun `UK long distance converts to miles`() {
    val d = 1600.0.toCarDistance(ukLocale)
    assertEquals(1600.0 / 1609.344, d.displayDistance, 0.001)
    assertEquals(Distance.UNIT_MILES, d.displayUnit)
  }

  // toCarDistanceToNextManeuver

  @Test
  fun `null progress produces zero distance`() {
    val distance: Distance = (null as TripProgress?).toCarDistanceToNextManeuver(siLocale)
    assertEquals(0.0, distance.displayDistance, 0.001)
  }

  @Test
  fun `SI progress produces correct distance to next maneuver in meters`() {
    val progress =
        TripProgress(
            distanceToNextManeuver = 450.0, distanceRemaining = 12000.0, durationRemaining = 600.0)
    val distance = progress.toCarDistanceToNextManeuver(siLocale)
    assertEquals(450.0, distance.displayDistance, 0.001)
    assertEquals(Distance.UNIT_METERS, distance.displayUnit)
  }

  // toCarTravelEstimate

  @Test
  fun `SI travel estimate converts remaining distance to km`() {
    val progress =
        TripProgress(
            distanceToNextManeuver = 200.0, distanceRemaining = 5000.0, durationRemaining = 300.0)
    val estimate = progress.toCarTravelEstimate(siLocale)
    assertEquals(5.0, estimate.remainingDistance!!.displayDistance, 0.001)
    assertEquals(Distance.UNIT_KILOMETERS, estimate.remainingDistance!!.displayUnit)
  }

  @Test
  fun `US travel estimate converts remaining distance to miles`() {
    val progress =
        TripProgress(
            distanceToNextManeuver = 200.0, distanceRemaining = 5000.0, durationRemaining = 300.0)
    val estimate = progress.toCarTravelEstimate(usLocale)
    assertEquals(5000.0 / 1609.344, estimate.remainingDistance!!.displayDistance, 0.001)
    assertEquals(Distance.UNIT_MILES, estimate.remainingDistance!!.displayUnit)
  }

  @Test
  fun `travel estimate has correct remaining time`() {
    val progress =
        TripProgress(
            distanceToNextManeuver = 200.0, distanceRemaining = 5000.0, durationRemaining = 300.0)
    val estimate = progress.toCarTravelEstimate(siLocale)
    assertEquals(300L, estimate.remainingTimeSeconds)
  }
}
