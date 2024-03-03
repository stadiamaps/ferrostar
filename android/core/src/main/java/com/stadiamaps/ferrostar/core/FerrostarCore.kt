package com.stadiamaps.ferrostar.core

import java.net.URL
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.NavigationController
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteAdapterInterface
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint

open class FerrostarCoreException : Exception {
  constructor(message: String) : super(message)

  constructor(message: String, cause: Throwable) : super(message, cause)

  constructor(cause: Throwable) : super(cause)
}

class InvalidStatusCodeException(val statusCode: Int) :
    FerrostarCoreException("Route request failed with status code $statusCode")

class NoResponseBodyException :
    FerrostarCoreException("Route request was successful but had no body bytes")

class UserLocationUnknown :
    FerrostarCoreException(
        "The user location is unknown; ensure the location provider is properly configured")

/** Corrective action to take when the user deviates from the route. */
sealed class CorrectiveAction {
  /**
   * Don't do anything.
   *
   * Note that this is most commonly paired with no route deviation tracking as a formality. Think
   * twice before using this as a mechanism for implementing your own logic outside of the provided
   * framework, as doing so will mean you miss out on state updates around alternate route
   * calculation.
   */
  object DoNothing : CorrectiveAction()

  /**
   * Tells the core to fetch new routes from the route adapter.
   *
   * Once they are available, the delegate will be notified of the new routes.
   */
  class GetNewRoutes(val waypoints: List<Waypoint>) : CorrectiveAction()
}

// TODO: Think of a better (more specialized) name for this interface; something about rerouting?
// The term "delegate" is imported from the Apple ecosystem and will be super confusing to a Kotlin
// dev.
interface FerrostarCoreDelegate {
  /**
   * Called when the core has loaded alternative routes.
   *
   * The developer may decide whether or not to act on this information given the current trip
   * state. This is currently used for recalculation when the user diverges from the route, but can
   * be extended for other uses in the future. Note that [FerrostarCoreState.isCalculatingNewRoute]
   * and [FerrostarCore.isCalculatingNewRoute] will be `true` until this method returns.
   */
  fun correctiveActionForDeviation(
      core: FerrostarCore,
      deviationInMeters: Double,
      remainingWaypoints: List<Waypoint>
  ): CorrectiveAction

  /**
   * Called when the core has loaded alternative routes.
   *
   * The developer may decide whether or not to act on this information given the current trip
   * state. This is currently used for recalculation when the user diverges from the route, but can
   * be extended for other uses in the future.
   */
  fun loadedAlternativeRoutes(core: FerrostarCore, routes: List<Route>)
}

data class FerrostarCoreState(
    /** The raw trip state from the core. */
    val tripState: TripState,
    /** Indicates when the core is calculating a new route (ex: due to the user being off route). */
    val isCalculatingNewRoute: Boolean
)

class FerrostarCore(
    val routeAdapter: RouteAdapterInterface,
    val httpClient: OkHttpClient,
    val locationProvider: LocationProvider,
    val delegate: FerrostarCoreDelegate?,
) : LocationUpdateListener {
  /**
   * The minimum time to wait before initiating another route recalculation.
   *
   * This matters in the case that a user is off route, the framework calculates a new route, and
   * the user is determined to still be off the new route. This adds a minimum delay (default 5
   * seconds).
   */
  var minimumTimeBeforeRecalculaton: Long = 5

  var isCalculatingNewRoute: Boolean = false
    private set

  private val _executor = Executors.newSingleThreadExecutor()
  private val _scope = CoroutineScope(Dispatchers.IO)
  private var _navigationController: NavigationController? = null
  private var _state: MutableStateFlow<FerrostarCoreState>? = null
  private var _routeRequestInFlight = false
  private var _lastAutomaticRecalculation: LocalDateTime? = null

  private var _config: NavigationControllerConfig? = null

  constructor(
      valhallaEndpointURL: URL,
      profile: String,
      httpClient: OkHttpClient,
      locationProvider: LocationProvider,
      delegate: FerrostarCoreDelegate?,
  ) : this(
      RouteAdapter.newValhallaHttp(valhallaEndpointURL.toString(), profile),
      httpClient,
      locationProvider,
      delegate,
  )

  suspend fun getRoutes(initialLocation: UserLocation, waypoints: List<Waypoint>): List<Route> =
      try {
        _routeRequestInFlight = true

        when (val request = routeAdapter.generateRequest(initialLocation, waypoints)) {
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

            routeAdapter.parseResponse(bodyBytes)
          }
        }
      } finally {
        // TODO: Make sure this doesn't cause issues when we add support for arbitrary code to
        // generate routes
        _routeRequestInFlight = false
      }

  /**
   * Starts navigation with the given parameters (erasing any previous state).
   *
   * Once you have a location fix and a desired route, invoke this method. It will automatically
   * subscribe to location provider updates. Returns a view model which
   */
  fun startNavigation(route: Route, config: NavigationControllerConfig): NavigationViewModel {
    stopNavigation()

    val controller =
        NavigationController(
            route,
            config,
        )
    val startingLocation = locationProvider.lastLocation ?: throw UserLocationUnknown()

    val initialTripState = controller.getInitialState(startingLocation.userLocation())
    val stateFlow = MutableStateFlow(FerrostarCoreState(tripState = initialTripState, false))
    handleStateUpdate(initialTripState, startingLocation.userLocation())

    _navigationController = controller
    _state = stateFlow

    locationProvider.addListener(this, _executor)

    return NavigationViewModel(stateFlow, startingLocation, route.geometry)
  }

  fun stopNavigation() {
    locationProvider.removeListener(this)
    _navigationController?.destroy()
    _navigationController = null
    _state = null
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
            _lastAutomaticRecalculation?.isAfter(
                LocalDateTime.now().minusSeconds(minimumTimeBeforeRecalculaton)) != false) {
          val action =
              delegate?.correctiveActionForDeviation(
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
                  if (delegate != null) {
                    delegate.loadedAlternativeRoutes(this@FerrostarCore, routes)
                  } else if (routes.count() > 1 && config != null) {
                    // Default behavior when there is no user-defined behavior:
                    // accept the first route, as this is what most users want when they go off
                    // route.
                    startNavigation(routes.first(), config)
                  }
                } finally {
                  _lastAutomaticRecalculation = LocalDateTime.now()
                  isCalculatingNewRoute = false
                }
              }
            }
          }
        }
      }
    }
  }

  override fun onLocationUpdated(location: Location) {
    val controller = _navigationController

    if (controller != null) {
      _state?.update { currentValue ->
        val newState =
            controller.updateUserLocation(
                location = location.userLocation(), state = currentValue.tripState)

        handleStateUpdate(newState, location.userLocation())

        FerrostarCoreState(tripState = newState, isCalculatingNewRoute)
      }
    }
  }

  override fun onHeadingUpdated(heading: Float) {
    // TODO: Publish new heading to flow
  }
}
