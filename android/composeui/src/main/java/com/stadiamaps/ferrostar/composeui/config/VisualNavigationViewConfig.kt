package com.stadiamaps.ferrostar.composeui.config

sealed class CameraControlState {
  data object Hidden : CameraControlState()

  data class ShowRecenter(val updateCamera: () -> Unit) : CameraControlState()

  data class ShowRouteOverview(val updateCamera: () -> Unit) : CameraControlState()
}

data class VisualNavigationViewConfig(
    var showMute: Boolean = false,
    var showZoom: Boolean = false,
    var cameraControlState: CameraControlState
) {
  companion object {
    fun Default() =
        VisualNavigationViewConfig(
            showMute = true, showZoom = true, cameraControlState = CameraControlState.Hidden)
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
