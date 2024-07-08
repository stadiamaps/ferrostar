package com.stadiamaps.ferrostar.composeui

import android.icu.util.ULocale
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDistanceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// NOTE: These tests have to be Android tests, since the ULocale APIs are not mocked.

/** Tests LocalizedDistanceFormatterTest behavior for a US English locale. */
@RunWith(AndroidJUnit4::class)
class LocalizedDistanceFormatterUSTest {
  private val formatter = LocalizedDistanceFormatter(localeOverride = ULocale.US)

  @Test
  fun distances_gt_1000mi_include_thousands_sep() {
    assertEquals("1,000 mi", formatter.format(1_609_344.0))
  }

  @Test
  fun distances_greater_than_10mi_rounded_to_nearest_mile() {
    assertEquals("11 mi", formatter.format(17380.0))
  }

  @Test
  fun distances_gt_1000ft_lt_10mi_have_at_most_1_fractional_digit() {
    assertEquals("0.2 mi", formatter.format(290.0))
    assertEquals("1 mi", formatter.format(1609.0))
    assertEquals("1.1 mi", formatter.format(1800.0))
  }

  @Test
  fun distances_lt_290m_formatted_as_feet() {
    assertEquals("900 ft", formatter.format(289.0))
    // Just over 850 ft; rounds up
    assertEquals("900 ft", formatter.format(260.0))
    // And just between 800f and 850ft will round down to 800ft
    assertEquals("800 ft", formatter.format(250.0))
  }

  @Test
  fun distances_between_100_and_500ft_rounded_to_nearest_multiple_of_50() {
    assertEquals("100 ft", formatter.format(32.0))
    assertEquals("150 ft", formatter.format(40.0))
  }

  @Test
  fun distances_between_50_and_100ft_rounded_to_nearest_multiple_of_10() {
    assertEquals("90 ft", formatter.format(27.5))
    assertEquals("60 ft", formatter.format(17.0))
  }

  @Test
  fun distances_lt_50ft_rounded_to_nearest_multiple_of_5() {
    assertEquals("40 ft", formatter.format(12.0))
    assertEquals("35 ft", formatter.format(10.0))
  }
}
