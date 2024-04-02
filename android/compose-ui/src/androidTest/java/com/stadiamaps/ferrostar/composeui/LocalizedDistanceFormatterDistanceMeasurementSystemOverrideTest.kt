package com.stadiamaps.ferrostar.composeui

import android.icu.util.ULocale
import org.junit.Assert
import org.junit.Test

// NOTE: These tests have to be Android tests, since the ULocale APIs are not mocked.

class LocalizedDistanceFormatterDistanceMeasurementSystemOverrideTest {
  @Test
  fun measurement_system_override_with_locale_override() {
    val formatter =
        LocalizedDistanceFormatter(
            localeOverride = ULocale.US,
            distanceMeasurementSystemOverride = DistanceMeasurementSystem.SI)
    Assert.assertEquals("17 km", formatter.format(17380.0))

    formatter.distanceMeasurementSystemOverride = null
    Assert.assertEquals("11 mi", formatter.format(17380.0))
  }

  @Test
  fun measurement_system_override_without_locale_override() {
    val formatter =
        LocalizedDistanceFormatter(distanceMeasurementSystemOverride = DistanceMeasurementSystem.SI)
    Assert.assertEquals("17 km", formatter.format(17380.0))

    formatter.distanceMeasurementSystemOverride = DistanceMeasurementSystem.IMPERIAL
    Assert.assertEquals("11 mi", formatter.format(17380.0))
  }
}
