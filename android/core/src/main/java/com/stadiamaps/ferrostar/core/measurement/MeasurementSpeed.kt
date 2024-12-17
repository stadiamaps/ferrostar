package com.stadiamaps.ferrostar.core.measurement

enum class MeasurementSpeedUnit {
  MetersPerSecond,
  MilesPerHour,
  KilometersPerHour,
  Knots
}

class MeasurementSpeed(val value: Double, val unit: MeasurementSpeedUnit) {

  companion object {
    // TODO: Move this to a shared conversions constants file?
    const val METERS_PER_SECOND_TO_MILES_PER_HOUR = 2.23694
    const val METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR = 3.6
    const val METERS_PER_SECOND_TO_KNOTS = 1.94384
  }

  fun value(converted: MeasurementSpeedUnit): Double {
    when (unit) {
      MeasurementSpeedUnit.MetersPerSecond -> {
        return when (converted) {
          MeasurementSpeedUnit.MetersPerSecond -> value
          MeasurementSpeedUnit.MilesPerHour -> value * METERS_PER_SECOND_TO_MILES_PER_HOUR
          MeasurementSpeedUnit.KilometersPerHour -> value * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR
          MeasurementSpeedUnit.Knots -> value * METERS_PER_SECOND_TO_KNOTS
        }
      }
      MeasurementSpeedUnit.MilesPerHour -> {
        return when (converted) {
          MeasurementSpeedUnit.MetersPerSecond -> value / METERS_PER_SECOND_TO_MILES_PER_HOUR
          MeasurementSpeedUnit.MilesPerHour -> value
          MeasurementSpeedUnit.KilometersPerHour ->
              value / METERS_PER_SECOND_TO_MILES_PER_HOUR * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR
          MeasurementSpeedUnit.Knots ->
              value / METERS_PER_SECOND_TO_MILES_PER_HOUR * METERS_PER_SECOND_TO_KNOTS
        }
      }
      MeasurementSpeedUnit.KilometersPerHour -> {
        return when (converted) {
          MeasurementSpeedUnit.MetersPerSecond -> value / METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR
          MeasurementSpeedUnit.MilesPerHour ->
              value / METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR * METERS_PER_SECOND_TO_MILES_PER_HOUR
          MeasurementSpeedUnit.KilometersPerHour -> value
          MeasurementSpeedUnit.Knots ->
              value / METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR * METERS_PER_SECOND_TO_KNOTS
        }
      }
      MeasurementSpeedUnit.Knots -> {
        return when (converted) {
          MeasurementSpeedUnit.MetersPerSecond -> value / METERS_PER_SECOND_TO_KNOTS
          MeasurementSpeedUnit.MilesPerHour ->
              value / METERS_PER_SECOND_TO_KNOTS * METERS_PER_SECOND_TO_MILES_PER_HOUR
          MeasurementSpeedUnit.KilometersPerHour ->
              value / METERS_PER_SECOND_TO_KNOTS * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR
          MeasurementSpeedUnit.Knots -> value
        }
      }
    }
  }
}
