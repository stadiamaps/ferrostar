package com.stadiamaps.ferrostar.core.annotation

import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit

data class AnnotationWrapper<T>(
    val annotation: T? = null,
    val speed: Speed? = null
) {
  val speedLimit: MeasurementSpeed?
    get() =
        when (speed) {
          is Speed.Value -> {
            when (speed.unit) {
              SpeedUnit.KILOMETERS_PER_HOUR ->
                  MeasurementSpeed(speed.value, MeasurementSpeedUnit.KilometersPerHour)
              SpeedUnit.MILES_PER_HOUR ->
                  MeasurementSpeed(speed.value, MeasurementSpeedUnit.MilesPerHour)
              SpeedUnit.KNOTS -> MeasurementSpeed(speed.value, MeasurementSpeedUnit.Knots)
            }
          }
          else -> null
        }

  companion object
}
