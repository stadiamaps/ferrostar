package com.stadiamaps.ferrostar.composeui

import android.icu.util.ULocale
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDistanceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

// NOTE: These tests have to be Android tests, since the ULocale APIs are not mocked.

/** Tests LocalizedDistanceFormatterTest behavior for a UK English locale. */
class LocalizedDistanceFormatterUKTest {
  private val formatter = LocalizedDistanceFormatter(localeOverride = ULocale.UK)

  @Test
  fun distances_gt_1000mi_include_thousands_sep() {
    assertEquals("1,000 mi", formatter.format(1_609_344.0))
  }

  @Test
  fun distances_greater_than_10mi_rounded_to_nearest_mile() {
    assertEquals("11 mi", formatter.format(17380.0))
  }

  @Test
  fun distances_gt_300m_and_lt_10m_have_at_most_one_fractional_digit() {
    assertEquals("0.2 mi", formatter.format(301.0))
    assertEquals("1 mi", formatter.format(1609.0))
    assertEquals("1.1 mi", formatter.format(1800.0))
  }

  @Test
  fun distances_between_10yd_and_300yd_round_to_nearest_multiple_of_10() {
    assertEquals("20 yd", formatter.format(20.0))
    assertEquals("300 yd", formatter.format(275.0))
  }

  @Test
  fun very_small_distances_rounded_to_nearest_multiple_of_5() {
    assertEquals("10 yd", formatter.format(10.0))
    assertEquals("5 yd", formatter.format(4.0))
    assertEquals("0 yd", formatter.format(2.0))
  }
}
