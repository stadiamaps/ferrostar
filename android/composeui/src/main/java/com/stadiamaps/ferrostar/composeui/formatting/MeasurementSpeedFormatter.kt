package com.stadiamaps.ferrostar.composeui.formatting

import android.content.Context
import android.icu.util.ULocale
import com.stadiamaps.ferrostar.composeui.measurement.localizedString
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeed
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit
import java.util.Locale

class MeasurementSpeedFormatter(context: Context, val measurementSpeed: MeasurementSpeed) {

  // This allows us to avoid capturing the context downstream
  private val unitLocalizations =
      mapOf(
          MeasurementSpeedUnit.MetersPerSecond to
              MeasurementSpeedUnit.MetersPerSecond.localizedString(context),
          MeasurementSpeedUnit.MilesPerHour to
              MeasurementSpeedUnit.MilesPerHour.localizedString(context),
          MeasurementSpeedUnit.KilometersPerHour to
              MeasurementSpeedUnit.KilometersPerHour.localizedString(context),
          MeasurementSpeedUnit.Knots to MeasurementSpeedUnit.Knots.localizedString(context))

  fun formattedValue(locale: ULocale = ULocale.getDefault()): String {
    val locale = locale.let { Locale(it.language, it.country) }
    return String.format(locale = locale, "%.0f", measurementSpeed.value)
  }

  fun formattedValue(
      locale: ULocale = ULocale.getDefault(),
      converted: MeasurementSpeedUnit = measurementSpeed.unit
  ): String {
    val locale = locale.let { Locale(it.language, it.country) }
    return String.format(locale = locale, "%.0f", measurementSpeed.value(converted))
  }

  fun formatted(): String {
    return "${measurementSpeed.value} ${unitLocalizations[measurementSpeed.unit]}"
  }

  fun formatted(converted: MeasurementSpeedUnit): String {
    return "${measurementSpeed.value(converted)} ${unitLocalizations[converted]}"
  }
}
