package com.stadiamaps.ferrostar.composeui

import android.icu.util.LocaleData
import android.icu.util.ULocale
import org.junit.Assert
import org.junit.Test

// NOTE: These tests have to be Android tests, since the ULocale APIs are not mocked.

class LocalizedDistanceFormatterMeasurementSystemOverrideTest {
  @Test
  fun `measurement system override along with preferred locale override`() {
    val formatter =
        LocalizedDistanceFormatter(
            localeOverride = ULocale.US,
            measurementSystemOverride = LocaleData.MeasurementSystem.SI)
    Assert.assertEquals("17 km", formatter.format(17380.0))

    formatter.measurementSystemOverride = null
    Assert.assertEquals("11 mi", formatter.format(17380.0))
  }

  @Test
  fun `measurement system override without a preferred locale override`() {
    val formatter =
        LocalizedDistanceFormatter(measurementSystemOverride = LocaleData.MeasurementSystem.SI)
    Assert.assertEquals("17 km", formatter.format(17380.0))

    formatter.measurementSystemOverride = LocaleData.MeasurementSystem.US
    Assert.assertEquals("11 mi", formatter.format(17380.0))
  }
}
