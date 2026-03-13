package com.stadiamaps.ferrostar.auto

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator
import com.stadiamaps.ferrostar.AppModule

class DemoCarAppService : CarAppService() {

  override fun createHostValidator(): HostValidator =
      if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
      } else {
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
      }

  override fun onCreateSession(sessionInfo: SessionInfo): Session {
    return DemoCarAppSession()
  }
}

class DemoCarAppSession : Session() {
  override fun onCreateScreen(intent: Intent) = DemoNavigationScreen(carContext)
}
