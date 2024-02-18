package com.stadiamaps.ferrostar.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationController
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteAdapterInterface
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import java.net.URL
import java.time.LocalDateTime
import java.util.concurrent.Executors

open class FerrostarCoreException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

class InvalidStatusCodeException(val statusCode: Int): FerrostarCoreException("Route request failed with status code $statusCode")

class NoResponseBodyException: FerrostarCoreException("Route request was successful but had no body bytes")

sealed class CorrectiveAction {
    class DoNothing: CorrectiveAction()

    class GetNewRoutes(val waypoints: List<GeographicCoordinate>): CorrectiveAction()
}

interface FerrostarCoreDelegate {
    /**
     * Called when the core has loaded alternative routes.
     *
     * The developer may decide whether or not to act on this information given the current trip state.
     * This is currently used for recalculation when the user diverges from the route, but can be extended for other uses in the future.
     */
    fun correctiveActionForDeviation(core: FerrostarCore, deviationInMeters: Double): CorrectiveAction

    /**
     * Called when the core has loaded alternative routes.
     *
     * The developer may decide whether or not to act on this information given the current trip state.
     * This is currently used for recalculation when the user diverges from the route, but can be extended for other uses in the future.
     */
    fun loadedAlternativeRoutes(core: FerrostarCore, routes: List<Route>)
}

data class FerrostarCoreState(val tripState: TripState, val isCalculatingNewRoute: Boolean)

class FerrostarCore(
    val routeAdapter: RouteAdapterInterface,
    val httpClient: OkHttpClient,
    val delegate: FerrostarCoreDelegate?,
) : LocationUpdateListener {
    private val _executor = Executors.newSingleThreadExecutor()
    private val _scope = CoroutineScope(Dispatchers.Default)
    private var _locationProvider: LocationProvider? = null
    private var _navigationController: NavigationController? = null
    private var _state: MutableStateFlow<FerrostarCoreState>? = null
    private var _routeRequestInFlight = false
    private var _isCalculatingNewRoute = false
    private var _lastAutomaticRecalculation: LocalDateTime? = null

    constructor(
        valhallaEndpointURL: URL,
        profile: String,
        httpClient: OkHttpClient,
        delegate: FerrostarCoreDelegate?,
    ) : this(
        RouteAdapter.newValhallaHttp(
            valhallaEndpointURL.toString(), profile
        ),
        httpClient,
        delegate,
    )

    suspend fun getRoutes(
        initialLocation: UserLocation, waypoints: List<GeographicCoordinate>
    ): List<Route> = try {
        _routeRequestInFlight = true

        when (val request = routeAdapter.generateRequest(initialLocation, waypoints)) {
            is RouteRequest.HttpPost -> {
                val httpRequest = Request.Builder()
                    .url(request.url)
                    .post(request.body.toRequestBody())
                    .apply {
                        request.headers.map { (name, value) ->
                            header(name, value)
                        }
                    }
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
        // TODO: Make sure this doesn't cause issues when we add support for arbitrary code to generate routes
        _routeRequestInFlight = false
    }

    /**
     * Starts navigation with the given parameters (erasing any previous state).
     *
     * Once you have a location fix and a desired route, invoke this method.
     * It will automatically subscribe to location provider updates.
     * Returns a view model which
     */
    fun startNavigation(route: Route, config: NavigationControllerConfig, locationProvider: LocationProvider, startingLocation: Location): NavigationViewModel {
        stopNavigation()

        val controller = NavigationController(
            route,
            config,
        )
        val stateFlow = MutableStateFlow(FerrostarCoreState(tripState = controller.getInitialState(startingLocation.userLocation()), false))

        _navigationController = controller
        _state = stateFlow
        _locationProvider = locationProvider

        locationProvider.addListener(this, _executor)

        return NavigationViewModel(
            stateFlow,
            startingLocation,
            route.geometry
        )
    }

    fun stopNavigation() {
        _locationProvider?.removeListener(this)
        _locationProvider = null
        _navigationController?.destroy()
        _navigationController = null
        _state = null
    }

    override fun onLocationUpdated(location: Location) {
        val controller = _navigationController

        if (controller != null) {
            _state?.update { currentValue ->
                val newState = controller.updateUserLocation(
                    location = location.userLocation(),
                    state = currentValue.tripState
                )

                if (newState is TripState.Navigating) {
                    if (newState.deviation is RouteDeviation.OffRoute) {
                        if (!_routeRequestInFlight && _lastAutomaticRecalculation?.isAfter(LocalDateTime.now().minusSeconds(15)) != false) {
                            val action = delegate?.correctiveActionForDeviation(
                                this,
                                newState.deviation.deviationFromRouteLine
                            ) ?: CorrectiveAction.DoNothing()
                            when (action) {
                                is CorrectiveAction.DoNothing -> {
                                    // Do nothing
                                }

                                is CorrectiveAction.GetNewRoutes -> {
                                    _isCalculatingNewRoute = true
                                    _scope.launch {
                                        try {
                                            val routes =
                                                getRoutes(location.userLocation(), action.waypoints)
                                            _lastAutomaticRecalculation = LocalDateTime.now()
                                            delegate?.loadedAlternativeRoutes(
                                                this@FerrostarCore,
                                                routes
                                            )
                                        } finally {
                                            _isCalculatingNewRoute = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FerrostarCoreState(tripState = newState, _isCalculatingNewRoute)
            }
        }
    }

    override fun onHeadingUpdated(heading: Float) {
        // TODO: Publish new heading to flow
    }
}
