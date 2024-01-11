package com.stadiamaps.ferrostar.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationController
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteAdapterInterface
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.TripState
import uniffi.ferrostar.UserLocation
import java.net.URL
import java.util.concurrent.Executors

open class FerrostarCoreException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

class InvalidStatusCodeException(val statusCode: Int): FerrostarCoreException("Route request failed with status code $statusCode")

class NoResponseBodyException: FerrostarCoreException("Route request was successful but had no body bytes")

class FerrostarCore(
    val routeAdapter: RouteAdapterInterface,
    val httpClient: OkHttpClient
) : LocationUpdateListener {
    private val _executor = Executors.newSingleThreadExecutor()
    private var _locationProvider: LocationProvider? = null
    private var _navigationController: NavigationController? = null
    private var _state: MutableStateFlow<TripState>? = null

    constructor(
        valhallaEndpointURL: URL,
        profile: String,
        httpClient: OkHttpClient,
    ) : this(
        RouteAdapter.newValhallaHttp(
            valhallaEndpointURL.toString(), profile
        ),
        httpClient,
    )

    suspend fun getRoutes(
        initialLocation: UserLocation, waypoints: List<GeographicCoordinate>
    ): List<Route> =
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
        val stateFlow = MutableStateFlow(controller.getInitialState(startingLocation.userLocation()))

        _navigationController = controller
        _state = stateFlow
        _locationProvider = locationProvider

        locationProvider.addListener(this, _executor)

        // TODO: hooks for things like recalculating

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
                controller.updateUserLocation(
                    location = location.userLocation(),
                    state = currentValue
                )
            }
        }
    }

    override fun onHeadingUpdated(heading: Float) {
        // TODO: Publish new heading to flow
    }
}
