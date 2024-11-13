package com.stadiamaps.ferrostar.composeui.config

data class VisualNavigationViewConfig(
    // Mute
    var showMute: Boolean = false,
    var onMute: (() -> Unit)? = null,

    // Zoom
    var showZoom: Boolean = false,
    var onZoomIn: (() -> Unit)? = null,
    var onZoomOut: (() -> Unit)? = null,
) {
  companion object {
    fun Default() = VisualNavigationViewConfig(showMute = true, showZoom = true)
  }
}

/** Enables the mute button in the navigation view. */
fun VisualNavigationViewConfig.useMuteButton(onMute: () -> Unit): VisualNavigationViewConfig {
  return copy(showMute = true, onMute = onMute)
}

/** Enables the zoom button in the navigation view. */
fun VisualNavigationViewConfig.useZoomButton(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
): VisualNavigationViewConfig {
  return copy(showZoom = true, onZoomIn = onZoomIn, onZoomOut = onZoomOut)
}
