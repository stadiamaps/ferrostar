package com.stadiamaps.ferrostar.core.annotation

enum class SpeedUnit(val text: String) {
  KILOMETERS_PER_HOUR("km/h"),
  MILES_PER_HOUR("mph"),
  KNOTS("knots");

  companion object {
    fun fromString(text: String): SpeedUnit? {
      return SpeedUnit.entries.firstOrNull { it.text == text }
    }
  }
}
