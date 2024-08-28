package com.stadiamaps.ferrostar

import android.app.Application
import com.ferrostar.carui.FerrostarCarAppApplication

class DemoApplication : Application(), FerrostarCarAppApplication {

  val appModule: AppModule by lazy { AppModule(this) }
  override val navigationViewModel = appModule.navigationViewModel
}
