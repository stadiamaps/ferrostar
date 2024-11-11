package com.stadiamaps.ferrostar.composeui.config

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

sealed class CameraControlState {
  data object Hidden : CameraControlState()

  data class ShowRecenter(val updateCamera: () -> Unit) : CameraControlState()

  data class ShowRouteOverview(val updateCamera: () -> Unit) : CameraControlState()
}

data class VisualNavigationViewConfig(
    var showMute: Boolean = false,
    var showZoom: Boolean = false,
    var buttonSize: DpSize = DpSize(56.dp, 56.dp)
data class VisualNavigationViewConfig(
    // Mute
    var showMute: Boolean = false,
    var onMute: (() -> Unit)? = null,
    // Zoom
    var showZoom: Boolean = false,
    var onZoomIn: (() -> Unit)? = null,
    var onZoomOut: (() -> Unit)? = null,
    // Center Camera
    // TODO: Not sure this is the best place for this bool since it's required.
//    var showCentering: Boolean = false,
    var onCenterLocation: (() -> Unit)? = null,
) {
  companion object {
    fun Default() = VisualNavigationViewConfig(showMute = true, showZoom = true)
  }
}

/** Enables the mute button in the navigation view. */
fun VisualNavigationViewConfig.useMuteButton(onMute: () -> Unit): VisualNavigationViewConfig {
  showMute = true
  this.onMute = onMute
  return this
}

/** Enables the zoom button in the navigation view. */
fun VisualNavigationViewConfig.useZoomButton(): VisualNavigationViewConfig {
  showZoom = true
  return this
}

/** Changes the size of navigation buttons. */
fun VisualNavigationViewConfig.buttonSize(size: DpSize): VisualNavigationViewConfig {
  buttonSize = size
fun VisualNavigationViewConfig.useZoomButton(
  onZoomIn: () -> Unit,
  onZoomOut: () -> Unit
): VisualNavigationViewConfig {
  showZoom = true
  this.onZoomIn = onZoomIn
  this.onZoomOut = onZoomOut
  return this
}
