package com.stadiamaps.ferrostar.core

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mock.MediaTypes
import okhttp3.mock.MockInterceptor
import okhttp3.mock.eq
import okhttp3.mock.get
import okhttp3.mock.post
import okhttp3.mock.respond
import okhttp3.mock.rule
import okhttp3.mock.url
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.RouteRequestGenerator
import uniffi.ferrostar.RouteResponseParser
import uniffi.ferrostar.UserLocation
import java.time.Instant

private val valhallaEndpointUrl = "https://api.stadiamaps.com/navigate/v1"

// Simple test to ensure that the extensibility with native code is working.

class MockRouteRequestGenerator: RouteRequestGenerator {
    override fun generateRequest(
        userLocation: UserLocation,
        waypoints: List<GeographicCoordinate>
    ): RouteRequest = RouteRequest.HttpPost(valhallaEndpointUrl, mapOf(), byteArrayOf())

}

class MockRouteResponseParser(private val routes: List<Route>) : RouteResponseParser {
    override fun parseResponse(response: ByteArray): List<Route> = routes
}

class FerrostarCoreTest {
    private val errorBody = """
        {
            "error": "No valid authentication provided."
        }
    """.trimIndent().toResponseBody(MediaTypes.MEDIATYPE_JSON)

    @Test
    fun test401UnauthorizedRouteResponse() = runTest {
        val interceptor = MockInterceptor().apply {
            rule(post, url eq valhallaEndpointUrl) {
                respond(401, errorBody)
            }

            rule(get) {
                respond {
                    throw IllegalStateException("an IO error")
                }
            }
        }

        val core = FerrostarCore(
            routeAdapter = RouteAdapter(requestGenerator = MockRouteRequestGenerator(), responseParser = MockRouteResponseParser(routes = listOf())),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        )

        try {
            // Tests that the core generates a request and attempts to process it, but throws due to the mocked network layer
            core.getRoutes(
                initialLocation = UserLocation(coordinates = GeographicCoordinate(-149.543469, 60.5347155), 0.0, null, Instant.now()),
                waypoints = listOf(GeographicCoordinate(-149.5485806, 60.5349908))
            )
            fail("Expected the request to fail")
        } catch (e: InvalidStatusCodeException) {
            assertEquals(401, e.statusCode)
        }
    }

    // TODO: successful test
}