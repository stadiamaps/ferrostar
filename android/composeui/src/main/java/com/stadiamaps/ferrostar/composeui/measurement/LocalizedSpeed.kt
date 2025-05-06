package com.stadiamaps.ferrostar.composeui.measurement

import android.content.Context
import com.stadiamaps.ferrostar.composeui.R
import com.stadiamaps.ferrostar.core.measurement.MeasurementSpeedUnit

fun MeasurementSpeedUnit.localizedString(context: Context): String {
  return when (this) {
    MeasurementSpeedUnit.MetersPerSecond -> context.getString(R.string.unit_short_mps)
    MeasurementSpeedUnit.MilesPerHour -> context.getString(R.string.unit_short_mph)
    MeasurementSpeedUnit.KilometersPerHour -> context.getString(R.string.unit_short_kph)
    MeasurementSpeedUnit.Knots -> context.getString(R.string.unit_short_knot)
  }
}
