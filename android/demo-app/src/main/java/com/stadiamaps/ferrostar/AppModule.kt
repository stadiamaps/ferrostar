package com.stadiamaps.ferrostar

import android.content.Context
import android.util.Log
import com.stadiamaps.ferrostar.composeui.notification.DefaultForegroundNotificationBuilder
import com.stadiamaps.ferrostar.core.AlternativeRouteProcessor
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.CorrectiveAction
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.RouteDeviationHandler
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import java.net.URL
import java.time.Duration
import okhttp3.OkHttpClient
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.StepAdvanceMode

/**
 * A basic sample of a dependency injection module for the demo app. This is only used to
 * demonstrate and test the basics of FerrostarCore with a dependency injection like stack. In a
 * real a app, use your preferred injection system.
 */
object AppModule {
  private const val TAG = "AppModule"

  private lateinit var appContext: Context

  fun init(context: Context) {
    appContext = context
  }

  val locationProvider: SimulatedLocationProvider by lazy { SimulatedLocationProvider() }
  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder().callTimeout(Duration.ofSeconds(15)).build()
  }

  private val foregroundServiceManager: ForegroundServiceManager by lazy {
    FerrostarForegroundServiceManager(appContext, DefaultForegroundNotificationBuilder(appContext))
  }

  // NOTE: This is a public instance which is suitable for development, but not for heavy use.
  // This server is suitable for testing and building your app, but once you are ready to go live,
  // YOU MUST USE ANOTHER SERVER.
  //
  // See https://github.com/stadiamaps/ferrostar/blob/main/VENDORS.md for options
  val ferrostarCore: FerrostarCore by lazy {
    val core =
        FerrostarCore(
            valhallaEndpointURL = URL("https://valhalla1.openstreetmap.de/route"),
            profile = "bicycle",
            httpClient = httpClient,
            locationProvider = locationProvider,
            foregroundServiceManager = foregroundServiceManager,
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.RelativeLineStringDistance(
                        minimumHorizontalAccuracy = 25U, automaticAdvanceDistance = 10U),
                    RouteDeviationTracking.StaticThreshold(25U, 10.0)),
            costingOptions = mapOf("bicycle" to mapOf("use_roads" to 0.2)))

    // Not all navigation apps will require this sort of extra configuration.
    // In fact, we hope that most don't!
    // In case you do though, this sample implementation shows what you'll need to get started
    // (this basically re-implements the default behaviors).
    core.deviationHandler = RouteDeviationHandler { _, _, remainingWaypoints ->
      CorrectiveAction.GetNewRoutes(remainingWaypoints)
    }

    core.alternativeRouteProcessor = AlternativeRouteProcessor { it, routes ->
      Log.i(TAG, "Received alternate route(s): $routes")
      if (routes.isNotEmpty()) {
        // NB: Use `replaceRoute` for cases like this!!
        it.replaceRoute(
            routes.first(),
            NavigationControllerConfig(
                StepAdvanceMode.RelativeLineStringDistance(
                    minimumHorizontalAccuracy = 25U, automaticAdvanceDistance = 10U),
                RouteDeviationTracking.StaticThreshold(25U, 10.0)))
      }
    }

    core
  }

  // The AndroidTtsObserver handles spoken instructions as they are triggered by FerrostarCore.
  val ttsObserver: AndroidTtsObserver by lazy { AndroidTtsObserver(appContext) }
}
