package com.stadiamaps.ferrostar.formatting

import com.stadiamaps.ferrostar.composeui.formatting.LocalizedDurationFormatter
import com.stadiamaps.ferrostar.composeui.formatting.UnitStyle
import kotlin.time.DurationUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedDurationFormatterTests {

  @Test
  fun testHourAndMinuteFormatShort() {
    val formatter =
        LocalizedDurationFormatter(units = listOf(DurationUnit.HOURS, DurationUnit.MINUTES))

    assertEquals("1h", formatter.format(3600.0))
    assertEquals("23h", formatter.format(82800.0))
    assertEquals("23h 59m", formatter.format(86340.0))
    assertEquals("25h 1m", formatter.format(90060.0))
    assertEquals("101h 1m", formatter.format(363660.0))
  }

  @Test
  fun testHourAndMinuteFormatLong() {
    val formatter =
        LocalizedDurationFormatter(
            units = listOf(DurationUnit.HOURS, DurationUnit.MINUTES), unitStyle = UnitStyle.LONG)

    assertEquals("1 hour", formatter.format(3600.0))
    assertEquals("23 hours", formatter.format(82800.0))
    assertEquals("23 hours 59 minutes", formatter.format(86340.0))
    assertEquals("25 hours 1 minute", formatter.format(90060.0))
    assertEquals("101 hours 1 minute", formatter.format(363660.0))
  }

  @Test
  fun testDaysHoursMinsSecsShort() {
    val formatter =
        LocalizedDurationFormatter(
            units =
                listOf(
                    DurationUnit.DAYS,
                    DurationUnit.HOURS,
                    DurationUnit.MINUTES,
                    DurationUnit.SECONDS))

    assertEquals("1d", formatter.format(86400.0))
    assertEquals("1d 1h", formatter.format(90000.0))
    assertEquals("1d 23h 59m", formatter.format(172740.0))
    assertEquals("10d 10h 14m 56s", formatter.format(900896.0))
  }

  @Test
  fun testDaysHoursMinsSecsLong() {
    val formatter =
        LocalizedDurationFormatter(
            units =
                listOf(
                    DurationUnit.DAYS,
                    DurationUnit.HOURS,
                    DurationUnit.MINUTES,
                    DurationUnit.SECONDS),
            unitStyle = UnitStyle.LONG)

    assertEquals("1 day", formatter.format(86400.0))
    assertEquals("1 day 1 hour", formatter.format(90000.0))
    assertEquals("1 day 23 hours 59 minutes", formatter.format(172740.0))
    assertEquals("10 days 10 hours 14 minutes 56 seconds", formatter.format(900896.0))
  }
}
