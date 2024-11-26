package com.stadiamaps.ferrostar.composeui.config

data class VisualNavigationViewConfig(
    // Mute
    var showMute: Boolean = false,

    // Zoom
    var showZoom: Boolean = false,
) {
  companion object {
    fun Default() = VisualNavigationViewConfig(showMute = true, showZoom = true)
  }
}

/** Enables the mute button in the navigation view. */
fun VisualNavigationViewConfig.useMuteButton(): VisualNavigationViewConfig {
  return copy(showMute = true)
}

/** Enables the zoom button in the navigation view. */
fun VisualNavigationViewConfig.useZoomButton(): VisualNavigationViewConfig {
  return copy(showZoom = true)
}
