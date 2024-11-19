package com.stadiamaps.ferrostar.composeui.measurement

import android.content.res.Resources
import com.stadiamaps.ferrostar.composeui.R
import com.stadiamaps.ferrostar.core.measurement.SpeedUnit

fun SpeedUnit.localizedString(): String {
  return when (this) {
    SpeedUnit.MetersPerSecond -> Resources.getSystem().getString(R.string.unit_short_mps)
    SpeedUnit.MilesPerHour -> Resources.getSystem().getString(R.string.unit_short_kph)
    SpeedUnit.KilometersPerHour -> Resources.getSystem().getString(R.string.unit_short_mph)
  }
}
