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
import com.stadiamaps.ferrostar.core.RoutingEngine
import com.stadiamaps.ferrostar.core.SimulatedLocationProvider
import com.stadiamaps.ferrostar.core.http.HttpClientProvider
import com.stadiamaps.ferrostar.core.http.OkHttpClientProvider.Companion.toOkHttpClientProvider
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import com.stadiamaps.ferrostar.googleplayservices.FusedLocationProvider
import java.time.Duration
import java.time.Instant
import okhttp3.OkHttpClient
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.GraphHopperVoiceUnits
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.UserLocation

/**
 * A basic sample of a dependency injection module for the demo app. This is only used to
 * demonstrate and test the basics of FerrostarCore with a dependency injection like stack. In a
 * real a app, use your preferred injection system.
 */
object AppModule {
  private const val TAG = "AppModule"

  private lateinit var appContext: Context

  // Here we show examples of how to use Ferrostar with a routing API.
  //
  // See https://stadiamaps.github.io/ferrostar/vendors.html for a list of vendors
  // known to work with Ferrostar.
  //
  // Option 1: Stadia Maps
  //
  // You can get an API key (free for development and evaluation; no credit card required)
  // at client.stadiamaps.com.
  // NOTE: The demo app requires a Stadia Maps API key for the search box to work.
  //
  // Add a line to your local.properties file to enable Stadia Maps:
  // stadiaApiKey=YOUR-API-KEY
  val stadiaApiKey =
      if (BuildConfig.stadiaApiKey.isBlank() || BuildConfig.stadiaApiKey == "null") {
        null
      } else {
        BuildConfig.stadiaApiKey
      }

  // Option 2: GraphHopper
  //
  // GraphHopper offers free API keys at https://www.graphhopper.com/.
  // Add a line to your local.properties file to enable GraphHopper:
  // graphhopperApiKey=YOUR-API-KEY
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

  fun init(context: Context) {
    appContext = context
  }

  // TODO: Make this configurable in the UI.
  val simulation = false
  val locationProvider: LocationProvider by lazy {
    if (simulation) {
      SimulatedLocationProvider().apply {
        warpFactor = 2u
        lastLocation =
            UserLocation(GeographicCoordinate(51.049315, 13.73552), 1.0, null, Instant.now(), null)
      }
    } else {
      FusedLocationProvider(appContext)
    }
  }
  private val httpClient: HttpClientProvider by lazy {
    OkHttpClient.Builder().callTimeout(Duration.ofSeconds(15)).build().toOkHttpClientProvider()
  }

  private val foregroundServiceManager: ForegroundServiceManager by lazy {
    FerrostarForegroundServiceManager(appContext, DefaultForegroundNotificationBuilder(appContext))
  }

  val ferrostarCore: FerrostarCore by lazy {
    // Option 1: Valhalla-based API
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

    val valhallaEndpoint: String by lazy {
      if (stadiaApiKey != null) {
        // If you have set a Stadia Maps API key in local.properties (see above instructions)
        "https://api.stadiamaps.com/route/v1?api_key=$stadiaApiKey"
      } else {
        // Fall back to the public FOSSGIS server
        "https://valhalla1.openstreeetmap.de/route"
      }
    }
    var engine: RoutingEngine = RoutingEngine.Valhalla(valhallaEndpoint, "auto")

    // GraphHopper API is used instead of valhalla if graphhopperApiKey is specified in
    // local.properties (see above instructions)
    if (graphhopperApiKey != null) {
      engine =
          RoutingEngine.GraphHopper(
              "https://graphhopper.com/api/1/navigate/?key=$graphhopperApiKey",
              profile = "car",
              locale = "en",
              voiceUnits = GraphHopperVoiceUnits.METRIC)

      // use default profile (no custom models)
      options = mapOf()

      // GraphHopper also supports custom models.
      // You can find the documentation here: https://docs.graphhopper.com/openapi/custom-model
      // Arbitrary example (limits the top speed on motorways to 100kph):
      //      options =
      //          mapOf(
      //              "ch.disable" to true,
      //              "custom_model" to
      //                  mapOf(
      //                      "distance_influence" to 15,
      //                      "speed" to
      //                          listOf(mapOf("if" to "road_class == MOTORWAY", "limit_to" to
      // "100"))))
    }
    val core =
        FerrostarCore(
            engine,
            httpClient = httpClient,
            locationProvider = locationProvider,
            foregroundServiceManager = foregroundServiceManager,
            navigationControllerConfig = NavigationControllerConfig.demoConfig(),
            options =
                mapOf(
                    "costing_options" to
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

  val viewModel: DemoNavigationViewModel by lazy { DemoNavigationViewModel() }
}
