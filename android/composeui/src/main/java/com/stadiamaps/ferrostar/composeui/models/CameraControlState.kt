package com.stadiamaps.ferrostar.composeui.models

sealed class CameraControlState {
  data object Hidden : CameraControlState()

  data class ShowRecenter(val updateCamera: () -> Unit) : CameraControlState()

  data class ShowRouteOverview(val updateCamera: () -> Unit) : CameraControlState()
}