package com.stadiamaps.ferrostar.core

import java.net.URL
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.Heading
import uniffi.ferrostar.NavigationController
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint

data class FerrostarCoreState(
    /** The raw trip state from the core. */
    val tripState: TripState,
    /** Indicates when the core is calculating a new route (ex: due to the user being off route). */
    val isCalculatingNewRoute: Boolean
)

/**
 * This is the entrypoint for end users of Ferrostar on Android, and is responsible for "driving"
 * the navigation with location updates and other events.
 *
 * The usual flow is for callers to configure an instance of the core reuse the instance for as long
 * as it makes sense (necessarily somewhat app-specific). You can first call [getRoutes] to fetch a
 * list of possible routes asynchronously. After selecting a suitable route (either interactively by
 * the user or programmatically), call [startNavigation] to start a session.
 *
 * NOTE: It is the responsibility of the caller to ensure that the location manager is authorized to
 * access the user's location.
 */
class FerrostarCore(
    val routeProvider: RouteProvider,
    val httpClient: OkHttpClient,
    val locationProvider: LocationProvider,
) : LocationUpdateListener {
  /**
   * The minimum time to wait before initiating another route recalculation.
   *
   * This matters in the case that a user is off route, the framework calculates a new route, and
   * the user is determined to still be off the new route. This adds a minimum delay (default 5
   * seconds).
   */
  var minimumTimeBeforeRecalculaton: Long = 5

  /**
   * Controls what happens when the user deviates from the route.
   *
   * The default behavior (when this property is `null`) is to fetch new routes automatically. These
   * will be passed to the [alternativeRouteProcessor] or, if none is specified, navigation will
   * automatically proceed according to the first route.
   */
  var deviationHandler: RouteDeviationHandler? = null

  /**
   * Handles alternative routes as they are loaded.
   *
   * The default behavior (when this property is `null`) is to automatically reroute the user when
   * an alternative route arrives due to the user being off course. In all other cases, no action
   * will be taken unless an [AlternativeRouteProcessor] is provided.
   */
  var alternativeRouteProcessor: AlternativeRouteProcessor? = null

  /**
   * Handles spoken instructions as they are triggered throughout navigation.
   *
   * The default behavior (when this property is `null`) is to do nothing. The bundled
   * [AndroidTtsObserver] can be easily configured though for an implementation with sensible
   * defaults. You will probably want to set the locale to match that of your directions.
   *
   * Note that [FerrostarCore] ensures that observers will not see the same instruction twice in the
   * course of a navigation session (that is, the period from [startNavigation] to
   * [stopNavigation]).
   */
  var spokenInstructionObserver: SpokenInstructionObserver? = null

  // Maintains a set of utterance IDs which have been seen previously.
  // This helps us maintain the guarantee that the observer won't see the same one twice.
  private val _queuedUtteranceIds: MutableSet<String> = mutableSetOf()

  var isCalculatingNewRoute: Boolean = false
    private set

  private val _executor = Executors.newSingleThreadExecutor()
  private val _scope = CoroutineScope(Dispatchers.IO)
  private var _navigationController: NavigationController? = null
  private var _state: MutableStateFlow<FerrostarCoreState>? = null
  private var _routeRequestInFlight = false
  private var _lastAutomaticRecalculation: Long? = null
  private var _lastLocation: UserLocation? = null

  private var _config: NavigationControllerConfig? = null

  constructor(
      valhallaEndpointURL: URL,
      profile: String,
      httpClient: OkHttpClient,
      locationProvider: LocationProvider,
      costingOptions: Map<String, Map<String, String>> = emptyMap(),
  ) : this(
      RouteProvider.RouteAdapter(
          RouteAdapter.newValhallaHttp(valhallaEndpointURL.toString(), profile, costingOptions)),
      httpClient,
      locationProvider,
  )

  constructor(
      routeAdapter: RouteAdapter,
      httpClient: OkHttpClient,
      locationProvider: LocationProvider,
  ) : this(
      RouteProvider.RouteAdapter(routeAdapter),
      httpClient,
      locationProvider,
  )

  constructor(
      customRouteProvider: CustomRouteProvider,
      httpClient: OkHttpClient,
      locationProvider: LocationProvider,
  ) : this(
      RouteProvider.CustomProvider(customRouteProvider),
      httpClient,
      locationProvider,
  )

  suspend fun getRoutes(initialLocation: UserLocation, waypoints: List<Waypoint>): List<Route> =
      try {
        _routeRequestInFlight = true

        when (routeProvider) {
          is RouteProvider.CustomProvider ->
              routeProvider.provider.getRoutes(initialLocation, waypoints)
          is RouteProvider.RouteAdapter -> {
            when (val request = routeProvider.adapter.generateRequest(initialLocation, waypoints)) {
              is RouteRequest.HttpPost -> {
                val httpRequest =
                    Request.Builder()
                        .url(request.url)
                        .post(request.body.toRequestBody())
                        .apply { request.headers.map { (name, value) -> header(name, value) } }
                        .build()

                val res = httpClient.newCall(httpRequest).await()
                val bodyBytes = res.body?.bytes()
                if (!res.isSuccessful) {
                  throw InvalidStatusCodeException(res.code)
                } else if (bodyBytes == null) {
                  throw NoResponseBodyException()
                }

                routeProvider.adapter.parseResponse(bodyBytes)
              }
            }
          }
        }
      } finally {
        _routeRequestInFlight = false
      }

  /**
   * Starts a navigation session with the given parameters (erasing any previous state).
   *
   * Once you have a location fix and a desired route, invoke this method. It will automatically
   * subscribe to location provider updates. Returns a view model which is tied to the navigation
   * session. You can observe this in either your own or one of the provided navigation compose
   * views.
   */
  @Throws(UserLocationUnknown::class)
  fun startNavigation(route: Route, config: NavigationControllerConfig): NavigationViewModel {
    stopNavigation()

    val controller =
        NavigationController(
            route,
            config,
        )
    val startingLocation = locationProvider.lastLocation ?: throw UserLocationUnknown()

    val initialTripState = controller.getInitialState(startingLocation)
    val stateFlow = MutableStateFlow(FerrostarCoreState(tripState = initialTripState, false))
    handleStateUpdate(initialTripState, startingLocation)

    _navigationController = controller
    _state = stateFlow

    locationProvider.addListener(this, _executor)

    return NavigationViewModel(stateFlow, startingLocation, route.geometry)
  }

  fun advanceToNextStep() {
    val controller = _navigationController
    val location = _lastLocation

    if (controller != null && location != null) {
      _state?.update { currentValue ->
        val newState = controller.advanceToNextStep(state = currentValue.tripState)

        handleStateUpdate(newState, location)

        FerrostarCoreState(tripState = newState, isCalculatingNewRoute)
      }
    }
  }

  fun stopNavigation() {
    locationProvider.removeListener(this)
    _navigationController?.destroy()
    _navigationController = null
    _state = null
    _queuedUtteranceIds.clear()
  }

  /**
   * Internal method to react to state updates.
   *
   * This is where reactions are triggered in response to a state change (ex: initiating
   * recalculation as the user goes off route).
   */
  private fun handleStateUpdate(newState: TripState, location: UserLocation) {
    if (newState is TripState.Navigating) {
      if (newState.deviation is RouteDeviation.OffRoute) {
        if (!_routeRequestInFlight &&
            _lastAutomaticRecalculation?.let {
              System.nanoTime() - it > minimumTimeBeforeRecalculaton
            } != false) {
          val action =
              deviationHandler?.correctiveActionForDeviation(
                  this, newState.deviation.deviationFromRouteLine, newState.remainingWaypoints)
                  ?: CorrectiveAction.GetNewRoutes(newState.remainingWaypoints)
          when (action) {
            is CorrectiveAction.DoNothing -> {
              // Do nothing
            }
            is CorrectiveAction.GetNewRoutes -> {
              isCalculatingNewRoute = true
              _scope.launch {
                try {
                  val routes = getRoutes(location, action.waypoints)
                  val config = _config
                  val processor = alternativeRouteProcessor
                  val state = _state?.value
                  // Make sure we are still navigating and the new route is still relevant
                  if (state != null &&
                      state.tripState is TripState.Navigating &&
                      state.tripState.deviation is RouteDeviation.OffRoute) {
                    if (processor != null) {
                      processor.loadedAlternativeRoutes(this@FerrostarCore, routes)
                    } else if (routes.count() > 1 && config != null) {
                      // Default behavior when there is no user-defined behavior:
                      // accept the first route, as this is what most users want when they go off
                      // route.
                      startNavigation(routes.first(), config)
                    }
                  }
                } finally {
                  _lastAutomaticRecalculation = System.nanoTime()
                  isCalculatingNewRoute = false
                }
              }
            }
          }
        }
      }

      if (newState.spokenInstruction != null) {
        if (!_queuedUtteranceIds.contains(newState.spokenInstruction.utteranceId)) {
          _queuedUtteranceIds.add(newState.spokenInstruction.utteranceId)
          spokenInstructionObserver?.onSpokenInstructionTrigger(newState.spokenInstruction)
        }
      }
    }
  }

  override fun onLocationUpdated(location: UserLocation) {
    _lastLocation = location
    val controller = _navigationController

    if (controller != null) {
      _state?.update { currentValue ->
        val newState =
            controller.updateUserLocation(location = location, state = currentValue.tripState)

        handleStateUpdate(newState, location)

        FerrostarCoreState(tripState = newState, isCalculatingNewRoute)
      }
    }
  }

  override fun onHeadingUpdated(heading: Heading) {
    // TODO: Publish new heading to flow
  }
}
