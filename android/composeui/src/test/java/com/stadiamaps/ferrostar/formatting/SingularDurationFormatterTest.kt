package com.stadiamaps.ferrostar.formatting

import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDurationFormatter
import com.stadiamaps.ferrostar.composeui.formatting.UnitStyle
import kotlin.time.DurationUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class SingularDurationFormatterTest {

  @Test
  fun testSecondsFormatShort() {
    val formatter = LocalizedDurationFormatter(units = listOf(DurationUnit.SECONDS))

    assertEquals("1s", formatter.format(1.0))
    assertEquals("59s", formatter.format(59.0))
    assertEquals("5999s", formatter.format(5999.0))
  }

  @Test
  fun testSecondsFormatLong() {
    val formatter =
        LocalizedDurationFormatter(units = listOf(DurationUnit.SECONDS), unitStyle = UnitStyle.LONG)

    assertEquals("1 second", formatter.format(1.0))
    assertEquals("59 seconds", formatter.format(59.0))
    assertEquals("5999 seconds", formatter.format(5999.0))
  }

  @Test
  fun testMinutesFormatShort() {
    val formatter = LocalizedDurationFormatter(units = listOf(DurationUnit.MINUTES))

    assertEquals("1m", formatter.format(60.0))
    assertEquals("59m", formatter.format(3540.0))
    assertEquals("5999m", formatter.format(359940.0))
  }

  @Test
  fun testMinutesFormatLong() {
    val formatter =
        LocalizedDurationFormatter(units = listOf(DurationUnit.MINUTES), unitStyle = UnitStyle.LONG)

    assertEquals("1 minute", formatter.format(60.0))
    assertEquals("59 minutes", formatter.format(3540.0))
    assertEquals("5999 minutes", formatter.format(359940.0))
  }

  @Test
  fun testHoursFormatShort() {
    val formatter = LocalizedDurationFormatter(units = listOf(DurationUnit.HOURS))

    assertEquals("1h", formatter.format(3600.0))
    assertEquals("23h", formatter.format(82800.0))
    assertEquals("5999h", formatter.format(21596400.0))
  }

  @Test
  fun testHoursFormatLong() {
    val formatter =
        LocalizedDurationFormatter(units = listOf(DurationUnit.HOURS), unitStyle = UnitStyle.LONG)

    assertEquals("1 hour", formatter.format(3600.0))
    assertEquals("23 hours", formatter.format(82800.0))
    assertEquals("5999 hours", formatter.format(21596400.0))
  }

  @Test
  fun testDaysFormatShort() {
    val formatter = LocalizedDurationFormatter(units = listOf(DurationUnit.DAYS))

    assertEquals("1d", formatter.format(86400.0))
    assertEquals("6d", formatter.format(518400.0))
    assertEquals("5999d", formatter.format(518313600.0))
  }

  @Test
  fun testDaysFormatLong() {
    val formatter =
        LocalizedDurationFormatter(units = listOf(DurationUnit.DAYS), unitStyle = UnitStyle.LONG)

    assertEquals("1 day", formatter.format(86400.0))
    assertEquals("6 days", formatter.format(518400.0))
    assertEquals("5999 days", formatter.format(518313600.0))
  }
}
