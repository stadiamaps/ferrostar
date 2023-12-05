package com.stadiamaps.ferrostar.core

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteAdapterInterface
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.UserLocation
import java.net.URL

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
) {
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
}
