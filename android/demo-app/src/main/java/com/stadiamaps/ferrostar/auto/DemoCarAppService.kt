package com.stadiamaps.ferrostar.auto

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator
import com.stadiamaps.ferrostar.AppModule
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntent
import com.stadiamaps.ferrostar.car.app.intent.CarAppMapIntentParser

class DemoCarAppService : CarAppService() {

  override fun createHostValidator(): HostValidator =
      if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
      } else {
        // For reference:
        //        HostValidator.Builder(applicationContext)
        //            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
        //            .build()
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
      }

  override fun onCreateSession(sessionInfo: SessionInfo): Session {
    return DemoCarAppSession()
  }
}

class DemoCarAppSession : Session() {
  override fun onCreateScreen(intent: Intent): Screen {
    AppModule.init(carContext)
    val screen = DemoNavigationScreen(carContext)
    handleIntent(intent)
    return screen
  }

  override fun onNewIntent(intent: Intent) = handleIntent(intent)

  // The most naive handling for each case.
  // See https://developer.android.com/training/cars/apps/navigation#support-navigation-intents
  private fun handleIntent(intent: Intent) {
    when (val mapIntent = CarAppMapIntentParser.parse(intent)) {
      // The demo ignores the action, mode, and avoid values.
      is CarAppMapIntent.Navigate ->
          when (val destination = mapIntent.destination) {
            is CarAppMapIntent.Navigate.Destination.Coordinate ->
                AppModule.viewModel.startNavigation(destination.location, destination.label)
            // A real app geocodes the query, then starts navigation.
            is CarAppMapIntent.Navigate.Destination.Query -> Unit
          }
      is CarAppMapIntent.Pin ->
          AppModule.viewModel.startNavigation(mapIntent.location, mapIntent.label)
      // A real app can move the map, show search results, or tell the user that it cannot help.
      is CarAppMapIntent.DisplayMap,
      is CarAppMapIntent.Search,
      is CarAppMapIntent.StreetView,
      null -> Unit
    }
  }
}
