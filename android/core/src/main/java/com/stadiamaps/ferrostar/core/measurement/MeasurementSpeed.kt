package com.stadiamaps.ferrostar.core.measurement

enum class SpeedUnit {
  MetersPerSecond,
  MilesPerHour,
  KilometersPerHour
}

class MeasurementSpeed(val value: Double, val unit: SpeedUnit) {

  companion object {
    // TODO: Move this to a shared conversions constants file?
    const val METERS_PER_SECOND_TO_MILES_PER_HOUR = 2.23694
    const val METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR = 3.6
  }

  fun value(converted: SpeedUnit): Double {
    when (unit) {
      SpeedUnit.MetersPerSecond -> {
        return when (converted) {
          SpeedUnit.MetersPerSecond -> value
          SpeedUnit.MilesPerHour -> value * METERS_PER_SECOND_TO_MILES_PER_HOUR
          SpeedUnit.KilometersPerHour -> value * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR
        }
      }
      SpeedUnit.MilesPerHour -> {
        return when (converted) {
          SpeedUnit.MetersPerSecond -> value / METERS_PER_SECOND_TO_MILES_PER_HOUR
          SpeedUnit.MilesPerHour -> value
          SpeedUnit.KilometersPerHour ->
              value / METERS_PER_SECOND_TO_MILES_PER_HOUR * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR
        }
      }
      SpeedUnit.KilometersPerHour -> {
        return when (converted) {
          SpeedUnit.MetersPerSecond -> value / METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR
          SpeedUnit.MilesPerHour ->
              value / METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR * METERS_PER_SECOND_TO_MILES_PER_HOUR
          SpeedUnit.KilometersPerHour -> value
        }
      }
    }
  }
}
