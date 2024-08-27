package com.ferrostar.carui

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.stadiamaps.ferrostar.core.NavigationViewModel

// TODO: Move this
interface FerrostarCarAppApplication {
  val navigationViewModel: NavigationViewModel
}

class FerrostarCarAppService(): CarAppService() {
  override fun createHostValidator(): HostValidator {
    return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
  }

  override fun onCreateSession(): Session {
    val ferrostarApplication = application as? FerrostarCarAppApplication ?: throw IllegalStateException("Application must implement FerrostarCarApplication to use FerrostarCarAppService")
    val viewModel = ferrostarApplication.navigationViewModel
    return FerrostarCarAppSessionFactory.create(viewModel)
  }
}

