package com.stadiamaps.ferrostar.maplibreui.config

data class VisualNavigationViewConfig(
    var showMute: Boolean = false,
    var showZoom: Boolean = false
) {
  companion object {
    fun Default() = VisualNavigationViewConfig(showMute = true, showZoom = true)
  }
}

/** Enables the mute button in the navigation view. */
fun VisualNavigationViewConfig.useMuteButton(): VisualNavigationViewConfig {
  showMute = true
  return this
}

/** Enables the zoom button in the navigation view. */
fun VisualNavigationViewConfig.useZoomButton(): VisualNavigationViewConfig {
  showZoom = true
  return this
}
