package com.stadiamaps.ferrostar.composeui.formatting

import android.icu.util.ULocale
import com.stadiamaps.ferrostar.composeui.measurement.localizedString
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.SpeedUnit
import java.util.Locale

class MeasurementSpeedFormatter(val measurementSpeed: MeasurementSpeed) {

  fun formattedValue(locale: ULocale = ULocale.getDefault()): String {
    val locale = locale.let { Locale(it.language, it.country) }
    return String.format(locale = locale, "%.0f", measurementSpeed.value)
  }

  fun formattedValue(
      locale: ULocale = ULocale.getDefault(),
      converted: SpeedUnit = measurementSpeed.unit
  ): String {
    val locale = locale.let { Locale(it.language, it.country) }
    return String.format(locale = locale, "%.0f", measurementSpeed.value(converted))
  }

  fun formatted(): String {
    return "${measurementSpeed.value} ${measurementSpeed.unit.localizedString()}"
  }

  fun formatted(converted: SpeedUnit): String {
    return "${measurementSpeed.value(converted)} ${converted.localizedString()}"
  }
}
