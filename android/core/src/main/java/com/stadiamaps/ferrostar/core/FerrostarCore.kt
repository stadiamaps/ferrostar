package com.stadiamaps.ferrostar.core

import com.stadiamaps.ferrostar.core.http.HttpClientProvider
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import java.time.Instant
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Heading
import uniffi.ferrostar.NavState
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.NavigationSession
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Uuid
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WellKnownRouteProvider

/** Represents the complete state of the navigation session provided by FerrostarCore-RS. */
data class NavigationState(
    /** The raw trip state from the core. */
    val tripState: TripState = TripState.Idle(null),
    val routeGeometry: List<GeographicCoordinate> = emptyList(),
    /** Indicates when the core is calculating a new route (ex: due to the user being off route). */
    val isCalculatingNewRoute: Boolean = false
) {
  companion object
}

fun NavigationState.isNavigating(): Boolean =
    when (tripState) {
      is TripState.Complete,
      is TripState.Idle -> false

      is TripState.Navigating -> true
    }

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
    val httpClient: HttpClientProvider,
    val locationProvider: LocationProvider,
    val foregroundServiceManager: ForegroundServiceManager? = null,
    navigationControllerConfig: NavigationControllerConfig,
    val sessionBuilder: FerrostarSessionBuilder =
        FerrostarSessionBuilder(navigationControllerConfig),
) : LocationUpdateListener {
  companion object {
    private const val TAG = "FerrostarCore"
  }

  /**
   * The minimum time to wait before initiating another route recalculation.
   *
   * This matters in the case that a user is off route, the framework calculates a new route, and
   * the user is determined to still be off the new route. This adds a minimum delay (default 5
   * seconds).
   */
  var minimumTimeBeforeRecalculation: Long = 5

  /**
   * The minimum distance (in meters) the user must move before performing another route
   * recalculation.
   *
   * This ensures that, while the user remains off the route, we don't keep triggering useless
   * recalculations.
   */
  var minimumMovementBeforeRecalculation = 50.0

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
  private val _queuedUtteranceIds: MutableSet<Uuid> = mutableSetOf()

  var isCalculatingNewRoute: Boolean = false
    private set

  private val _executor = Executors.newSingleThreadScheduledExecutor()
  private val _scope = CoroutineScope(Dispatchers.IO)

  private var _navigationSession: NavigationSession? = null
  private val _navState: MutableStateFlow<NavState?> = MutableStateFlow(null)
  private var _state: MutableStateFlow<NavigationState> = MutableStateFlow(NavigationState())
  private var _routeRequestInFlight = false
  private var _lastAutomaticRecalculation: Long? = null
  private var _lastLocation: UserLocation? = null

  // The last location from which we triggered a recalculation
  private var _lastRecalculationLocation: UserLocation? = null

  private var _config: NavigationControllerConfig = navigationControllerConfig

  /**
   * The current state of the navigation session. This can be used in a custom ViewModel or
   * elsewhere. If using the default behavior, use the DefaultNavigationViewModel by injection or as
   * provided by startNavigation().
   */
  var state: StateFlow<NavigationState> = _state.asStateFlow()

  constructor(
      wellKnownRouteProvider: WellKnownRouteProvider,
      httpClient: HttpClientProvider,
      locationProvider: LocationProvider,
      navigationControllerConfig: NavigationControllerConfig,
      foregroundServiceManager: ForegroundServiceManager? = null,
  ) : this(
      RouteProvider.RouteAdapter(RouteAdapter.fromWellKnownRouteProvider(wellKnownRouteProvider)),
      httpClient,
      locationProvider,
      foregroundServiceManager,
      navigationControllerConfig)

  constructor(
      routeAdapter: RouteAdapter,
      httpClient: HttpClientProvider,
      locationProvider: LocationProvider,
      navigationControllerConfig: NavigationControllerConfig,
      foregroundServiceManager: ForegroundServiceManager? = null,
  ) : this(
      RouteProvider.RouteAdapter(routeAdapter),
      httpClient,
      locationProvider,
      foregroundServiceManager,
      navigationControllerConfig)

  constructor(
      customRouteProvider: CustomRouteProvider,
      httpClient: HttpClientProvider,
      locationProvider: LocationProvider,
      navigationControllerConfig: NavigationControllerConfig,
      foregroundServiceManager: ForegroundServiceManager? = null,
  ) : this(
      RouteProvider.CustomProvider(customRouteProvider),
      httpClient,
      locationProvider,
      foregroundServiceManager,
      navigationControllerConfig)

  suspend fun getRoutes(initialLocation: UserLocation, waypoints: List<Waypoint>): List<Route> =
      try {
        _routeRequestInFlight = true

        when (routeProvider) {
          is RouteProvider.CustomProvider ->
              routeProvider.provider.getRoutes(initialLocation, waypoints)

          is RouteProvider.RouteAdapter -> {
            val routeRequest = routeProvider.adapter.generateRequest(initialLocation, waypoints)

            val res = httpClient.call(routeRequest)
            val bodyBytes = res.bodyBytes()
            if (!res.isSuccessful) {
              throw InvalidStatusCodeException(res.code)
            } else if (bodyBytes == null) {
              throw NoResponseBodyException()
            }

            routeProvider.adapter.parseResponse(bodyBytes)
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
   *
   * WARNING: If you want to reuse the existing view model, ex: when getting a new route after going
   * off course, use [replaceRoute] instead! Otherwise, you will miss out on updates as the old view
   * model is "orphaned"!
   *
   * @param route the route to navigate.
   * @param config change the configuration in the core before staring navigation. This was
   *   originally provided on init, but you can set a new value for future sessions.
   * @throws UserLocationUnknown if the location provider has no last known location.
   */
  @Throws(UserLocationUnknown::class)
  fun startNavigation(route: Route, config: NavigationControllerConfig? = null) {
    stopNavigation()

    // Start the foreground notification service
    foregroundServiceManager?.startService(this::stopNavigation)

    // Apply the new config if provided, otherwise use the original.
    _config = config ?: _config

    val navigationSession = sessionBuilder.build(route, config)
    _navigationSession = navigationSession

    val startingLocation =
        locationProvider.lastLocation
            ?: UserLocation(route.geometry.first(), 0.0, null, Instant.now(), null)

    val initialNavState = navigationSession.getInitialState(startingLocation)
    val newState = NavigationState(tripState = initialNavState.tripState, route.geometry, false)
    handleStateUpdate(initialNavState, startingLocation)

    _navState.value = initialNavState
    _state.value = newState

    locationProvider.addListener(this, _executor)
  }

  /**
   * Resumes a previously started navigation session from the last known state.
   *
   * Important! This feature is experimental and may exhibit unexpected behavior. Please report any
   * issues you encounter to help us improve it.
   *
   * @throws NoCachedSession if there is no cached session to resume from.
   * @throws UserLocationUnknown if the location provider has no last known location.
   */
  fun resumeNavigation() {
    stopNavigation()

    // Start the foreground notification service
    foregroundServiceManager?.startService(this::stopNavigation)

    val (navigationSession, route, navState) = sessionBuilder.buildResumedSession()
    _navigationSession = navigationSession

    val startingLocation =
        locationProvider.lastLocation
            ?: UserLocation(route.geometry.first(), 0.0, null, Instant.now(), null)

    val newState = NavigationState(tripState = navState.tripState, route.geometry, false)
    handleStateUpdate(navState, startingLocation)

    _navState.value = navState
    _state.value = newState

    locationProvider.addListener(this, _executor)
  }

  /**
   * Replace the currently running route with a new one.
   *
   * This allows you to reuse the existing view model. Do not call this method unless you are
   * already navigating.
   *
   * @param route the route to navigate.
   * @param config change the configuration in the core before staring navigation. This was
   *   originally provided on init, but you can set a new value for future sessions.
   */
  fun replaceRoute(route: Route, config: NavigationControllerConfig? = null) {
    // Apply the new config if provided, otherwise use the original.
    _config = config ?: _config

    val navigationSession = sessionBuilder.build(route, config)
    _navigationSession = navigationSession

    val startingLocation =
        locationProvider.lastLocation
            ?: UserLocation(route.geometry.first(), 0.0, null, Instant.now(), null)

    _queuedUtteranceIds.clear()
    spokenInstructionObserver?.stopAndClearQueue()

    val newState = navigationSession.getInitialState(startingLocation)

    handleStateUpdate(newState, startingLocation)

    _navState.update { newState }
    _state.update { NavigationState(tripState = newState.tripState, route.geometry, false) }
  }

  fun advanceToNextStep() {
    val session = _navigationSession
    val location = _lastLocation

    if (session != null && location != null) {
      _navState.value?.let {
        val newState = session.advanceToNextStep(state = it)
        handleStateUpdate(newState, location)

        _navState.update { newState }
        _state.update { currentState ->
          NavigationState(
              tripState = newState.tripState, currentState.routeGeometry, isCalculatingNewRoute)
        }
      }
    }
  }

  fun stopNavigation(stopLocationUpdates: Boolean = true) {
    foregroundServiceManager?.stopService()
    if (stopLocationUpdates) {
      locationProvider.removeListener(this)
    }
    _navigationSession?.destroy()
    _navigationSession = null
    _state.value = NavigationState()
    _queuedUtteranceIds.clear()
    spokenInstructionObserver?.stopAndClearQueue()
    _lastRecalculationLocation = null
  }

  /**
   * Internal method to react to state updates.
   *
   * This is where reactions are triggered in response to a state change (ex: initiating
   * recalculation as the user goes off route).
   */
  private fun handleStateUpdate(newState: NavState, location: UserLocation) {
    val tripState = newState.tripState
    if (tripState is TripState.Navigating) {
      if (tripState.deviation is RouteDeviation.OffRoute) {
        if (!_routeRequestInFlight && // We can't have a request in flight already
            _lastAutomaticRecalculation?.let {
              // Ensure a minimum cool down before a new route fetch
              System.nanoTime() - it > minimumTimeBeforeRecalculation
            } != false &&
            _lastRecalculationLocation?.let {
              // Don't recalculate again if the user hasn't moved much
              it.toAndroidLocation().distanceTo(location.toAndroidLocation()) >
                  minimumMovementBeforeRecalculation
            } != false) {
          val action =
              deviationHandler?.correctiveActionForDeviation(
                  this, tripState.deviation.deviationFromRouteLine, tripState.remainingWaypoints)
                  ?: CorrectiveAction.GetNewRoutes(tripState.remainingWaypoints)
          when (action) {
            is CorrectiveAction.DoNothing -> {
              // Do nothing
            }

            is CorrectiveAction.GetNewRoutes -> {
              isCalculatingNewRoute = true
              _lastRecalculationLocation = location
              _scope.launch {
                try {
                  val routes = getRoutes(location, action.waypoints)
                  val config = _config
                  val processor = alternativeRouteProcessor
                  val state = _state.value
                  // Make sure we are still navigating and the new route is still relevant
                  if (state.tripState is TripState.Navigating &&
                      state.tripState.deviation is RouteDeviation.OffRoute) {
                    if (processor != null) {
                      processor.loadedAlternativeRoutes(this@FerrostarCore, routes)
                    } else if (routes.isNotEmpty()) {
                      // Default behavior when there is no user-defined behavior:
                      // accept the first route, as this is what most users want when they go off
                      // route.
                      replaceRoute(routes.first(), config)
                    }
                  }
                } catch (e: Throwable) {
                  android.util.Log.e(TAG, "Failed to recalculate route: $e")
                } finally {
                  _lastAutomaticRecalculation = System.nanoTime()
                  isCalculatingNewRoute = false
                }
              }
            }
          }
        }
      }

      if (tripState.spokenInstruction != null) {
        if (!_queuedUtteranceIds.contains(tripState.spokenInstruction.utteranceId)) {
          _queuedUtteranceIds.add(tripState.spokenInstruction.utteranceId)
          spokenInstructionObserver?.onSpokenInstructionTrigger(tripState.spokenInstruction)
        }
      }
    }

    // Update the notification manager (this propagates the state to the notification)
    foregroundServiceManager?.onNavigationStateUpdated(_state.value)
  }

  override fun onLocationUpdated(location: UserLocation) {
    _lastLocation = location
    val session = _navigationSession

    if (session != null) {
      _navState.value?.let {
        val newState = session.updateUserLocation(location = location, state = it)
        handleStateUpdate(newState, location)

        _navState.update { newState }
        _state.update { currentState ->
          NavigationState(
              tripState = newState.tripState, currentState.routeGeometry, isCalculatingNewRoute)
        }
      }
    }
  }

  override fun onHeadingUpdated(heading: Heading) {
    // TODO: Publish new heading to flow
  }
}
