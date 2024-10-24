package com.stadiamaps.ferrostar

import android.content.Context
import android.util.Log
import com.stadiamaps.ferrostar.composeui.notification.DefaultForegroundNotificationBuilder
import com.stadiamaps.ferrostar.core.AlternativeRouteProcessor
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.CorrectiveAction
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.LocationProvider
import com.stadiamaps.ferrostar.core.RouteDeviationHandler
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import com.stadiamaps.ferrostar.googleplayservices.FusedLocationProvider
import java.net.URL
import java.time.Duration
import okhttp3.OkHttpClient
import uniffi.ferrostar.CourseFiltering
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

  // You can get an API key (free for development and evaluation; no credit card required)
  // at client.stadiamaps.com.
  // You can also modify this file to use your preferred sources for maps and/or routing.
  // See https://stadiamaps.github.io/ferrostar/vendors.html for vendors known to work with
  // Ferrostar.
  //
  // NOTE: Don't set this directly in source code. Add a line to your local.properties file:
  // stadiaApiKey=YOUR-API-KEY
  val stadiaApiKey =
      if (BuildConfig.stadiaApiKey.isBlank() || BuildConfig.stadiaApiKey == "null") {
        null
      } else {
        BuildConfig.stadiaApiKey
      }

  val mapStyleUrl: String by lazy {
    if (stadiaApiKey != null)
        "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=$stadiaApiKey"
    else "https://demotiles.maplibre.org/style.json"
  }

  val valhallaEndpointUrl: URL by lazy {
    if (stadiaApiKey != null) {
      URL("https://api.stadiamaps.com/route/v1?api_key=$stadiaApiKey")
    } else {
      URL("https://valhalla1.openstreeetmap.de/route")
    }
  }

  fun init(context: Context) {
    appContext = context
  }

  val locationProvider: LocationProvider by lazy {
    // TODO: Make this configurable?
    FusedLocationProvider(appContext)
  }
  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder().callTimeout(Duration.ofSeconds(15)).build()
  }

  private val foregroundServiceManager: ForegroundServiceManager by lazy {
    FerrostarForegroundServiceManager(appContext, DefaultForegroundNotificationBuilder(appContext))
  }

  val ferrostarCore: FerrostarCore by lazy {
    val core =
        FerrostarCore(
            valhallaEndpointURL = valhallaEndpointUrl,
            profile = "auto",
            httpClient = httpClient,
            locationProvider = locationProvider,
            foregroundServiceManager = foregroundServiceManager,
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.RelativeLineStringDistance(
                        minimumHorizontalAccuracy = 25U, automaticAdvanceDistance = 10U),
                    RouteDeviationTracking.StaticThreshold(15U, 50.0),
                    CourseFiltering.SNAP_TO_ROUTE),
            options =
                mapOf(
                    "costingOptions" to
                        // Just an example... You can set multiple costing options for any profile
                        // in Valhalla.
                        // If your app uses multiple routing modes, you can have a master settings
                        // map, or construct a new one each time.
                        mapOf(
                            "low_speed_vehicle" to
                                mapOf(
                                    "vehicle_type" to "golf_cart",
                                    "top_speed" to 32 // 24kph ~= 15mph
                                    )),
                    "units" to "miles"))

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
        // NB: Use `replaceRoute` for cases like this!
        it.replaceRoute(routes.first())
      }
    }

    core
  }

  // The AndroidTtsObserver handles spoken instructions as they are triggered by FerrostarCore.
  val ttsObserver: AndroidTtsObserver by lazy { AndroidTtsObserver(appContext) }
}
