package com.stadiamaps.ferrostar.core.extensions

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import uniffi.ferrostar.TripProgress

/**
 * The estimated arrival date and time.
 *
 * @param fromDate The starting instant, defaults to the current time and can be adjusted for
 *   testing.
 * @param timeZone The time zone to use for the calculation, defaults to the system default.
 */
fun TripProgress.estimatedArrivalTime(
    fromDate: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
  val period =
      DateTimePeriod(days = 0, hours = 0, minutes = 0, seconds = this.durationRemaining.toInt())
  return fromDate.plus(period, timeZone).toLocalDateTime(timeZone)
}
