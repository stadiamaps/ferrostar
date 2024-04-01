package com.stadiamaps.ferrostar.composeui

import android.icu.util.ULocale
import org.junit.Assert
import org.junit.Test

// NOTE: These tests have to be Android tests, since the ULocale APIs are not mocked.

/** Tests LocalizedDistanceFormatterTest behavior for an German locale. */
class LocalizedDistanceFormatterDETest {
  private val formatter = LocalizedDistanceFormatter(localeOverride = ULocale.GERMANY)

  @Test
  fun `distances greater than 1000km include a thousands separator`() {
    // NOTE: Germans use a . rather than a , as a thousands separator
    Assert.assertEquals("1.000 km", formatter.format(1_000_000.0))
  }

  @Test
  fun `distances greater than 10 km are rounded to the nearest km`() {
    Assert.assertEquals("10 km", formatter.format(10_450.0))
    Assert.assertEquals("11 km", formatter.format(10_550.0))
  }

  @Test
  fun `distances between 1km and 10km are represented with at most one digit after the point`() {
    Assert.assertEquals("1 km", formatter.format(1_005.0))
    Assert.assertEquals("8,1 km", formatter.format(8_145.0))
  }

  @Test
  fun `distances between 100m and 1km are rounded to the nearest multiple of 100`() {
    Assert.assertEquals("100 m", formatter.format(145.0))
    Assert.assertEquals("900 m", formatter.format(850.0))
  }

  @Test
  fun `distances between 10m and 100m are rounded to the nearest multiple of 10`() {
    Assert.assertEquals("10 m", formatter.format(14.0))
    Assert.assertEquals("90 m", formatter.format(85.0))
  }

  @Test
  fun `very small distances are rounded to the nearest multiple of 5`() {
    Assert.assertEquals("10 m", formatter.format(10.0))
    Assert.assertEquals("5 m", formatter.format(4.0))
    Assert.assertEquals("0 m", formatter.format(2.0))
  }
}
