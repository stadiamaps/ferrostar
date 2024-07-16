package com.stadiamaps.ferrostar.maplibreui.extensions

import com.maplibre.compose.camera.models.CameraPadding

/**
 * A camera padding configuration for a navigation map view that places the user puck
 * in the lower center of the screen.
 */
fun CameraPadding.Companion.NavigationPortrait(): CameraPadding {
  return CameraPadding(
      start = 0.0,
      top = 1300.0,
      end = 0.0,
      bottom = 0.0,
  )
}

/**
 * A camera padding configuration for a navigation map view that places the user puck
 * in the lower end (right) of the screen.
 */
fun CameraPadding.Companion.NavigationLandscape(): CameraPadding {
  return CameraPadding(
      start = 1000.0,
      top = 500.0,
      end = 0.0,
      bottom = 0.0,
  )
}
