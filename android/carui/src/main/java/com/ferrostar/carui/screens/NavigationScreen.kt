package com.ferrostar.carui.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import com.ferrostar.carui.FerrostarCarAppService
import com.ferrostar.carui.surface.NavigationMapRenderer
import com.stadiamaps.ferrostar.core.NavigationViewModel

class NavigationScreen(carContext: CarContext, viewModel: NavigationViewModel) :
    Screen(carContext) {
  private val mapRenderer = NavigationMapRenderer(carContext, viewModel)

  override fun onGetTemplate(): Template {
    val service = carContext.getCarServiceName(FerrostarCarAppService::class.java)

    val pane = Pane.Builder().setLoading(false).build()

    return PaneTemplate.Builder(pane).setHeaderAction(Action.BACK).setTitle("Navigation").build()
  }
}
