package com.stadiamaps.ferrostar.composeui

import android.icu.util.ULocale
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// NOTE: These tests have to be Android tests, since the ULocale APIs are not mocked.

/** Tests LocalizedDistanceFormatterTest behavior for a US English locale. */
@RunWith(AndroidJUnit4::class)
class LocalizedDistanceFormatterUSTest {
  private val formatter = LocalizedDistanceFormatter(localeOverride = ULocale.US)

  @Test
  fun `distances greater than 1000 miles include a thousands separator`() {
    assertEquals("1,000 mi", formatter.format(1_609_344.0))
  }

  @Test
  fun `distances greater than 10 miles are rounded to the nearest mile`() {
    assertEquals("11 mi", formatter.format(17380.0))
  }

  @Test
  fun `distances greater than 1000 feet but less that 10 miles are represented with at most one digit after the point`() {
    assertEquals("0.2 mi", formatter.format(290.0))
    assertEquals("1 mi", formatter.format(1609.0))
    assertEquals("1.1 mi", formatter.format(1800.0))
  }

  @Test
  fun `distances less than 290 meters are formatted as feet`() {
    assertEquals("900 ft", formatter.format(289.0))
    // Just over 850 ft; rounds up
    assertEquals("900 ft", formatter.format(260.0))
    // And just between 800f and 850ft will round down to 800ft
    assertEquals("800 ft", formatter.format(250.0))
  }

  @Test
  fun `distances between 100 and 500 feet are rounded to the nearest multiple of 50`() {
    assertEquals("100 ft", formatter.format(32.0))
    assertEquals("150 ft", formatter.format(40.0))
  }

  @Test
  fun `distances between 50 and 100 feet are rounded to the nearest multiple of 10`() {
    assertEquals("90 ft", formatter.format(27.5))
    assertEquals("60 ft", formatter.format(17.0))
  }

  @Test
  fun `distances less than 50 feet are rounded to the nearest multiple of 5`() {
    assertEquals("40 ft", formatter.format(12.0))
    assertEquals("35 ft", formatter.format(10.0))
  }
}
