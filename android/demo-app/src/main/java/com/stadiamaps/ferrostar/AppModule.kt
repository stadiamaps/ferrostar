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
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import com.stadiamaps.ferrostar.googleplayservices.FusedLocationProvider
import java.net.URL
import java.time.Duration
import java.time.Instant
import okhttp3.OkHttpClient
import uniffi.ferrostar.CourseFiltering
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.SpecialAdvanceConditions
import uniffi.ferrostar.StepAdvanceMode
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.WaypointAdvanceMode

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
  // and if you want to use the GraphHopper API for routing add additionally:
  // graphhopperApiKey=YOUR-API-KEY
  val stadiaApiKey =
      if (BuildConfig.stadiaApiKey.isBlank() || BuildConfig.stadiaApiKey == "null") {
        null
      } else {
        BuildConfig.stadiaApiKey
      }

  val graphhopperApiKey =
      if (BuildConfig.graphhopperApiKey.isBlank() || BuildConfig.graphhopperApiKey == "null") {
        null
      } else {
        BuildConfig.graphhopperApiKey
      }

  val mapStyleUrl: String by lazy {
    if (stadiaApiKey != null)
        "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=$stadiaApiKey"
    else "https://demotiles.maplibre.org/style.json"
  }

  val routingEndpointURL: URL by lazy {
    if (graphhopperApiKey != null) {
      URL("https://graphhopper.com/api/1/navigate/?key=$graphhopperApiKey")
    } else if (stadiaApiKey != null) {
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
    // SimulatedLocationProvider().apply {
    //  warpFactor = 2u
    //  lastLocation =
    //      UserLocation(GeographicCoordinate(51.049315, 13.73552), 1.0, null, Instant.now(), null)
    // }
  }
  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder().callTimeout(Duration.ofSeconds(15)).build()
  }

  private val foregroundServiceManager: ForegroundServiceManager by lazy {
    FerrostarForegroundServiceManager(appContext, DefaultForegroundNotificationBuilder(appContext))
  }

  val ferrostarCore: FerrostarCore by lazy {
    var options =
        mapOf(
            "costingOptions" to
                // Just an example... You can set multiple costing options for any profile
                // in Valhalla.
                // If your app uses multiple routing modes, you can have a master settings
                // map, or construct a new one each time.
                mapOf(
                    "low_speed_vehicle" to
                        mapOf(
                            "vehicle_type" to "golf_cart", "top_speed" to 32 // 24kph ~= 15mph
                            )),
            "units" to "miles")

    var routingEngine = "valhalla"
    var routingEngineProfile = "auto"

    // GraphHopper API is used instead of valhalla if graphhopperApiKey is specified in
    // local.properties
    if (graphhopperApiKey != null) {
      routingEngine = "graphhopper"
      routingEngineProfile = "car"

      if (true) {
        // use default profile (no custom models)
        options = mapOf()
      } else {
        // documentation for custom models: https://docs.graphhopper.com/openapi/custom-model
        // arbitrary example:
        options =
            mapOf(
                "ch.disable" to true,
                "custom_model" to
                    mapOf(
                        "distance_influence" to 15,
                        "speed" to
                            listOf(mapOf("if" to "road_class == MOTORWAY", "limit_to" to "100"))))
      }
    }
    val core =
        FerrostarCore(
            routingEndpointURL = routingEndpointURL,
            routingEngine = routingEngine,
            profile = routingEngineProfile,
            httpClient = httpClient,
            locationProvider = locationProvider,
            foregroundServiceManager = foregroundServiceManager,
            navigationControllerConfig =
                NavigationControllerConfig(
                    WaypointAdvanceMode.WaypointWithinRange(100.0),
                    StepAdvanceMode.RelativeLineStringDistance(
                        minimumHorizontalAccuracy = 25U,
                        specialAdvanceConditions =
                            // NOTE: We have not yet put this threshold through extensive real-world
                            // testing
                            SpecialAdvanceConditions.MinimumDistanceFromCurrentStepLine(10U)),
                    RouteDeviationTracking.StaticThreshold(15U, 50.0),
                    CourseFiltering.SNAP_TO_ROUTE),
            options = options)

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

  val viewModel: DemoNavigationViewModel by lazy { DemoNavigationViewModel() }
}
