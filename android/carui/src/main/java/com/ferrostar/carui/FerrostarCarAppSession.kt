package com.ferrostar.carui

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.ferrostar.carui.screens.NavigationScreen
import com.stadiamaps.ferrostar.core.NavigationViewModel

object FerrostarCarAppSessionFactory {
  fun create(viewModel: NavigationViewModel): FerrostarCarAppSession {
    return FerrostarCarAppSession(viewModel)
  }
}

class FerrostarCarAppSession(
  private val viewModel: NavigationViewModel
): Session() {

  override fun onCreateScreen(intent: Intent): Screen {
    return NavigationScreen(carContext, viewModel)
  }
}