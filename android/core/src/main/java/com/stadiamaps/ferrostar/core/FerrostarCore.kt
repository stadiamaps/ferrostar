package com.stadiamaps.ferrostar.core

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
import uniffi.ferrostar.StepAdvanceMode
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

public class FerrostarCore(
    val routeAdapter: RouteAdapterInterface,
    val locationProvider: LocationProvider,
    val httpClient: OkHttpClient
) : LocationUpdateListener {
    private var navigationController: NavigationController? = null

    constructor(
        valhallaEndpointURL: URL,
        profile: String,
        locationProvider: LocationProvider,
        httpClient: OkHttpClient,
    ) : this(
        RouteAdapter.newValhallaHttp(
            valhallaEndpointURL.toString(), profile
        ),
        locationProvider,
        httpClient,
    )

    suspend fun getRoutes(
        initialLocation: UserLocation, waypoints: List<GeographicCoordinate>
    ): List<Route> {
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

                return routeAdapter.parseResponse(bodyBytes)
            }
        }
    }

    fun startNavigation(route: Route, stepAdvance: StepAdvanceMode, startingLocation: UserLocation) {
        // TODO: Is this the best executor?
        locationProvider.addListener(this, Executors.newSingleThreadExecutor())

        // TODO: Init view model
        navigationController = NavigationController(
            lastUserLocation = startingLocation,
            route = route,
            config = NavigationControllerConfig(stepAdvance = stepAdvance)
        )
    }

    fun stopNavigation() {
        navigationController = null
        // TODO: Clear ViewModel
        // TODO: Is this the best executor?
        locationProvider.removeListener(this)
    }

    override fun onLocationUpdated(location: Location) {
        // TODO: Update view model and navigation controller
        TODO("Not yet implemented")
    }

    override fun onHeadingUpdated(heading: Float) {
        // TODO: Update view model
        TODO("Not yet implemented")
    }
}
