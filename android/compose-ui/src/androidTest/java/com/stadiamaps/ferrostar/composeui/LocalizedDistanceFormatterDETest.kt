package com.stadiamaps.ferrostar.composeui

import android.icu.util.ULocale
import org.junit.Assert
import org.junit.Test

// NOTE: These tests have to be Android tests, since the ULocale APIs are not mocked.

/** Tests LocalizedDistanceFormatterTest behavior for an German locale. */
class LocalizedDistanceFormatterDETest {
  private val formatter = LocalizedDistanceFormatter(localeOverride = ULocale.GERMANY)

  @Test
  fun distances_gt_100m_include_thousands_separator() {
    // NOTE: Germans use a . rather than a , as a thousands separator
    Assert.assertEquals("1.000 km", formatter.format(1_000_000.0))
  }

  @Test
  fun distances_gt_10km_rounded_to_nearest_km() {
    Assert.assertEquals("10 km", formatter.format(10_450.0))
    Assert.assertEquals("11 km", formatter.format(10_550.0))
  }

  @Test
  fun distances_between_1km_and_10km_have_at_most_1_fractional_digit() {
    Assert.assertEquals("1 km", formatter.format(1_005.0))
    Assert.assertEquals("8,1 km", formatter.format(8_145.0))
  }

  @Test
  fun distances_between_100m_and_1km_rounded_to_nearest_multiple_of_100() {
    Assert.assertEquals("100 m", formatter.format(145.0))
    Assert.assertEquals("900 m", formatter.format(850.0))
  }

  @Test
  fun distances_between_10m_and_100m_rounded_to_nearest_multiple_of_10() {
    Assert.assertEquals("10 m", formatter.format(14.0))
    Assert.assertEquals("90 m", formatter.format(85.0))
  }

  @Test
  fun very_small_distances_rounded_to_nearest_multiple_of_5() {
    Assert.assertEquals("10 m", formatter.format(10.0))
    Assert.assertEquals("5 m", formatter.format(4.0))
    Assert.assertEquals("0 m", formatter.format(2.0))
  }
}
