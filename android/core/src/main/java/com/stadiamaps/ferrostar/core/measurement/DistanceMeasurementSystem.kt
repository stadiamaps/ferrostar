package com.stadiamaps.ferrostar.core.measurement

import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build

/** Measurement system model that backports Java APIs not available before API 28. */
enum class DistanceMeasurementSystem {
  /** Metric system; used by most of the world. */
  SI,

  /** The US version of the imperial system which uses feet and miles as distance units. */
  IMPERIAL,

  /** The UK version of the imperial system which uses yards and miles as distance units. */
  IMPERIAL_WITH_YARDS
}

fun getMeasurementSystem(locale: ULocale): DistanceMeasurementSystem =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      when (LocaleData.getMeasurementSystem(locale) ?: LocaleData.MeasurementSystem.SI) {
        LocaleData.MeasurementSystem.US -> DistanceMeasurementSystem.IMPERIAL
        LocaleData.MeasurementSystem.UK -> DistanceMeasurementSystem.IMPERIAL_WITH_YARDS
        else -> DistanceMeasurementSystem.SI
      }
    } else {
      when (locale.isO3Country) {
        "USA",
        "LBR",
        "MMR" -> DistanceMeasurementSystem.IMPERIAL
        "GBR" -> DistanceMeasurementSystem.IMPERIAL_WITH_YARDS
        else -> DistanceMeasurementSystem.SI
      }
    }
