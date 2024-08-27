package com.ferrostar.carui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.compose.runtime.MutableState
import com.ferrostar.carui.FerrostarCarAppService
import com.ferrostar.carui.surface.NavigationMapRenderer
import com.maplibre.compose.camera.MapViewCamera
import com.stadiamaps.ferrostar.core.NavigationViewModel

class NavigationScreen(
  carContext: CarContext,
  viewModel: NavigationViewModel
) : Screen(carContext) {
  private val mapRenderer = NavigationMapRenderer(carContext, viewModel)

  override fun onGetTemplate(): Template {
    val service = carContext.getCarServiceName(FerrostarCarAppService::class.java)

    val pane = Pane.Builder()
      .setLoading(false)
      .build()

    return PaneTemplate.Builder(pane)
      .setHeaderAction(Action.BACK)
      .setTitle("Navigation")
      .build()
  }
}