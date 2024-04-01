package com.stadiamaps.ferrostar.composeui

import android.icu.util.ULocale
import org.junit.Assert.assertEquals
import org.junit.Test

// NOTE: These tests have to be Android tests, since the ULocale APIs are not mocked.

/** Tests LocalizedDistanceFormatterTest behavior for a UK English locale. */
class LocalizedDistanceFormatterUKTest {
  private val formatter = LocalizedDistanceFormatter(localeOverride = ULocale.UK)

  @Test
  fun `distances greater than 1000 miles include a thousands separator`() {
    assertEquals("1,000 mi", formatter.format(1_609_344.0))
  }

  @Test
  fun `distances greater than 10 miles are rounded to the nearest mile`() {
    assertEquals("11 mi", formatter.format(17380.0))
  }

  @Test
  fun `distances greater than 300m but less that 10 miles are represented with at most one digit after the point`() {
    assertEquals("0.2 mi", formatter.format(301.0))
    assertEquals("1 mi", formatter.format(1609.0))
    assertEquals("1.1 mi", formatter.format(1800.0))
  }

  @Test
  fun `distances between 10 and around 300 yards are rounded to the nearest multiple of 10`() {
    assertEquals("20 yd", formatter.format(20.0))
    assertEquals("300 yd", formatter.format(275.0))
  }

  @Test
  fun `very small distances are rounded to the nearest multiple of 5`() {
    assertEquals("10 yd", formatter.format(10.0))
    assertEquals("5 yd", formatter.format(4.0))
    assertEquals("0 yd", formatter.format(2.0))
  }
}
