package com.stadiamaps.ferrostar.maplibreui.config

import android.view.Gravity
import com.maplibre.compose.settings.AttributionSettings
import com.maplibre.compose.settings.CompassSettings
import com.maplibre.compose.settings.LogoSettings
import com.maplibre.compose.settings.MapControls
import com.maplibre.compose.settings.MarginInsets

/**
 * Returns the map controls for the given configuration.
 *
 * @param isLandscape whether the NavigationView is in landscape orientation.
 * @param isArrivalExpanded weather the arrival view is expanded.
 */
internal fun mapControlsFor(isLandscape: Boolean, isArrivalExpanded: Boolean): MapControls {
  val bottom =
      when {
        isLandscape -> 32
        isArrivalExpanded -> 32 * 9
        else -> 32 * 7
      }

  return MapControls(
      attribution =
          AttributionSettings(
              gravity = Gravity.BOTTOM or Gravity.END,
              margins = MarginInsets(end = 270, bottom = bottom)),
      compass = CompassSettings(enabled = false),
      logo =
          LogoSettings(
              gravity = Gravity.BOTTOM or Gravity.END,
              margins = MarginInsets(end = 32, bottom = bottom)))
}
