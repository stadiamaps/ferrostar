package com.stadiamaps.ferrostar.composeui.formatting

import kotlin.time.DurationUnit

enum class UnitStyle {
  SHORT,
  LONG
}

fun interface DurationFormatter {
  /** Formats a duration in seconds into a human-readable string. */
  fun format(durationSeconds: Double): String
}

class LocalizedDurationFormatter(
    private val units: List<DurationUnit> = listOf(DurationUnit.HOURS, DurationUnit.MINUTES),
    private val unitStyle: UnitStyle = UnitStyle.SHORT
) : DurationFormatter {

  private fun calculate(durationSeconds: Double): Map<DurationUnit, Int> {
    var remainingDuration = durationSeconds
    val result: MutableMap<DurationUnit, Int> = mutableMapOf()

    if (units.contains(DurationUnit.NANOSECONDS) ||
        units.contains(DurationUnit.MICROSECONDS) ||
        units.contains(DurationUnit.MILLISECONDS)) {
      throw IllegalArgumentException("Unsupported duration unit")
    }

    // Extract the days from the duration
    if (units.contains(DurationUnit.DAYS)) {
      val days = (remainingDuration / (24 * 60 * 60)).toInt()
      remainingDuration %= (24 * 60 * 60)
      result += DurationUnit.DAYS to days
    }

    // Extract the hours from the duration
    if (units.contains(DurationUnit.HOURS)) {
      val hours = (remainingDuration / (60 * 60)).toInt()
      remainingDuration %= (60 * 60)
      result += DurationUnit.HOURS to hours
    }

    // Extract the minutes from the duration
    if (units.contains(DurationUnit.MINUTES)) {
      val minutes = (remainingDuration / 60).toInt()
      remainingDuration %= 60
      result += DurationUnit.MINUTES to minutes
    }

    // Extract the seconds from the duration
    if (units.contains(DurationUnit.SECONDS)) {
      result += DurationUnit.SECONDS to remainingDuration.toInt()
    }

    // Return a map of the non-null values and their corresponding units
    return result
  }

  // TODO: Localize the unit strings
  private fun getUnitString(unit: DurationUnit, value: Int): String {
    val plural = if (value != 1) "s" else ""

    return when (unitStyle) {
      UnitStyle.SHORT ->
          when (unit) {
            DurationUnit.SECONDS -> "s"
            DurationUnit.MINUTES -> "m"
            DurationUnit.HOURS -> "h"
            DurationUnit.DAYS -> "d"
            else -> ""
          }
      UnitStyle.LONG ->
          when (unit) {
            DurationUnit.SECONDS -> " second$plural"
            DurationUnit.MINUTES -> " minute$plural"
            DurationUnit.HOURS -> " hour$plural"
            DurationUnit.DAYS -> " day$plural"
            else -> " "
          }
    }
  }

  override fun format(durationSeconds: Double): String {
    val durationMap = calculate(durationSeconds)

    return durationMap.entries
        .filter { it.value > 0 }
        .joinToString(separator = " ") { "${it.value}${getUnitString(it.key, it.value)}" }
  }
}
