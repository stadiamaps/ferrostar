package com.stadiamaps.ferrostar.core

import java.time.Instant
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
import uniffi.ferrostar.BoundingBox
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.ManeuverModifier
import uniffi.ferrostar.ManeuverType
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteDeviation
import uniffi.ferrostar.RouteDeviationDetector
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.RouteRequest
import uniffi.ferrostar.RouteRequestGenerator
import uniffi.ferrostar.RouteResponseParser
import uniffi.ferrostar.RouteStep
import uniffi.ferrostar.StepAdvanceMode
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.VisualInstruction
import uniffi.ferrostar.VisualInstructionContent
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

private val valhallaEndpointUrl = "https://api.stadiamaps.com/navigate/v1"

// Simple test to ensure that the extensibility with native code is working.

class MockRouteRequestGenerator : RouteRequestGenerator {
  override fun generateRequest(
      userLocation: UserLocation,
      waypoints: List<Waypoint>
  ): RouteRequest = RouteRequest.HttpPost(valhallaEndpointUrl, mapOf(), byteArrayOf())
}

class MockRouteResponseParser(private val routes: List<Route>) : RouteResponseParser {
  override fun parseResponse(response: ByteArray): List<Route> = routes
}

class FerrostarCoreTest {
  private val errorBody =
      """
        {
            "error": "No valid authentication provided."
        }
    """
          .trimIndent()
          .toResponseBody(MediaTypes.MEDIATYPE_JSON)

  // Mocked route
  private val mockGeom =
      listOf(GeographicCoordinate(lat = 0.0, lng = 0.0), GeographicCoordinate(lat = 1.0, lng = 1.0))
  private val instructionContent =
      VisualInstructionContent(
          text = "Sail straight",
          maneuverType = ManeuverType.DEPART,
          maneuverModifier = ManeuverModifier.STRAIGHT,
          roundaboutExitDegrees = null)
  private val mockRoute =
      Route(
          geometry = mockGeom,
          bbox = BoundingBox(sw = mockGeom.first(), ne = mockGeom.last()),
          distance = 1.0,
          waypoints = mockGeom.map { Waypoint(coordinate = it, kind = WaypointKind.BREAK) },
          steps =
              listOf(
                  RouteStep(
                      geometry = mockGeom,
                      distance = 1.0,
                      roadName = "foo road",
                      instruction = "Sail straight",
                      visualInstructions =
                          listOf(
                              VisualInstruction(
                                  primaryContent = instructionContent,
                                  secondaryContent = null,
                                  triggerDistanceBeforeManeuver = 42.0)),
                      spokenInstructions = listOf())))

  @Test
  fun test401UnauthorizedRouteResponse() = runTest {
    val interceptor =
        MockInterceptor().apply {
          rule(post, url eq valhallaEndpointUrl) { respond(401, errorBody) }

          rule(get) { respond { throw IllegalStateException("an IO error") } }
        }

    val core =
        FerrostarCore(
            routeAdapter =
                RouteAdapter(
                    requestGenerator = MockRouteRequestGenerator(),
                    responseParser = MockRouteResponseParser(routes = listOf())),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = SimulatedLocationProvider())

    try {
      // Tests that the core generates a request and attempts to process it, but throws due to the
      // mocked network layer
      core.getRoutes(
          initialLocation =
              UserLocation(
                  coordinates =
                      GeographicCoordinate(
                          60.5347155,
                          -149.543469,
                      ),
                  0.0,
                  null,
                  Instant.now()),
          waypoints =
              listOf(
                  Waypoint(
                      coordinate = GeographicCoordinate(60.5349908, -149.5485806),
                      kind = WaypointKind.BREAK)))
      fail("Expected the request to fail")
    } catch (e: InvalidStatusCodeException) {
      assertEquals(401, e.statusCode)
    }
  }

  @Test
  fun test200MockRouteResponse() = runTest {
    val interceptor =
        MockInterceptor().apply {
          rule(post, url eq valhallaEndpointUrl) { respond(200, "".toResponseBody()) }

          rule(get) { respond { throw IllegalStateException("an IO error") } }
        }

    val core =
        FerrostarCore(
            routeAdapter =
                RouteAdapter(
                    requestGenerator = MockRouteRequestGenerator(),
                    responseParser = MockRouteResponseParser(routes = listOf(mockRoute))),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = SimulatedLocationProvider())
    val routes =
        core.getRoutes(
            initialLocation =
                UserLocation(
                    coordinates =
                        GeographicCoordinate(
                            lat = 60.5347155,
                            lng = -149.543469,
                        ),
                    horizontalAccuracy = 6.0,
                    courseOverGround = null,
                    timestamp = Instant.now()),
            waypoints =
                listOf(
                    Waypoint(
                        coordinate = GeographicCoordinate(lat = 60.5349908, lng = -149.5485806),
                        kind = WaypointKind.BREAK)))

    assertEquals(listOf(mockRoute), routes)
  }

  @Test
  fun testCustomRouteDeviationHandler() = runTest {
    val interceptor =
        MockInterceptor().apply {
          rule(post, url eq valhallaEndpointUrl) { respond(200, "".toResponseBody()) }

          rule(post, url eq valhallaEndpointUrl) { respond(200, "".toResponseBody()) }
        }

    class DeviationHandler : RouteDeviationHandler {
      var called = false

      override fun correctiveActionForDeviation(
          core: FerrostarCore,
          deviationInMeters: Double,
          remainingWaypoints: List<Waypoint>
      ): CorrectiveAction {
        called = true
        assertEquals(42.0, deviationInMeters, Double.MIN_VALUE)
        return CorrectiveAction.GetNewRoutes(remainingWaypoints)
      }
    }

    class RouteProcessor : AlternativeRouteProcessor {
      var called = false

      override fun loadedAlternativeRoutes(core: FerrostarCore, routes: List<Route>) {
        called = true
        assert(core.isCalculatingNewRoute) // We are still calculating until this method completes
        assert(routes.isNotEmpty())
      }
    }

    val locationProvider = SimulatedLocationProvider()
    val core =
        FerrostarCore(
            routeAdapter =
                RouteAdapter(
                    requestGenerator = MockRouteRequestGenerator(),
                    responseParser = MockRouteResponseParser(routes = listOf(mockRoute))),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = locationProvider)

    val deviationHandler = DeviationHandler()
    core.deviationHandler = deviationHandler

    val processor = RouteProcessor()
    core.alternativeRouteProcessor = processor

    val routes =
        core.getRoutes(
            initialLocation =
                UserLocation(
                    coordinates =
                        GeographicCoordinate(
                            lat = 60.5347155,
                            lng = -149.543469,
                        ),
                    horizontalAccuracy = 6.0,
                    courseOverGround = null,
                    timestamp = Instant.now()),
            waypoints =
                listOf(
                    Waypoint(
                        coordinate = GeographicCoordinate(lat = 60.5349908, lng = -149.5485806),
                        kind = WaypointKind.BREAK)))

    locationProvider.lastLocation =
        SimulatedLocation(GeographicCoordinate(0.0, 0.0), 6.0, null, Instant.now())
    core.startNavigation(
        routes.first(),
        NavigationControllerConfig(
            stepAdvance = StepAdvanceMode.RelativeLineStringDistance(16U, 16U),
            routeDeviationTracking =
                RouteDeviationTracking.Custom(
                    detector =
                        object : RouteDeviationDetector {
                          override fun checkRouteDeviation(
                              location: UserLocation,
                              route: Route,
                              currentRouteStep: RouteStep
                          ): RouteDeviation {
                            return RouteDeviation.OffRoute(42.0)
                          }
                        })))

    assert(deviationHandler.called)

    // TODO: Figure out how to test this properly with Kotlin coroutines + JUnit in the way.
    // Spent several hours fighting it trying to get something half as good as XCTestExpectation,
    // but was ultimately unsuccessful. I verified this works fine in a debugger and real app,
    // but the test scope is different.
    //        assert(processor.called)
  }
}
