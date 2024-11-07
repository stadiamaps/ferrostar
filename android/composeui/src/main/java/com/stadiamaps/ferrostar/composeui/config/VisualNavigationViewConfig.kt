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

/** Changes the size of navigation buttons. */
fun VisualNavigationViewConfig.buttonSize(size: DpSize): VisualNavigationViewConfig {
  buttonSize = size
  return this
}
