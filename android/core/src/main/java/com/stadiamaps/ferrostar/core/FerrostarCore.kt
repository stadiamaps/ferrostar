package com.stadiamaps.ferrostar.core

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.GeographicCoordinates
import uniffi.ferrostar.NavigationController
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.UserLocation
import java.net.URL

class FerrostarCore(
    val routeAdapter: RouteAdapter,
    val locationProvider: LocationProvider,
    val httpClient: OkHttpClient
) {
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
        initialLocation: UserLocation, waypoints: List<GeographicCoordinates>
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
                    TODO("Throw a useful exception")
                } else if (bodyBytes == null) {
                    TODO("Throw a useful exception")
                }

                return routeAdapter.parseResponse(bodyBytes)
            }
        }
    }
}
