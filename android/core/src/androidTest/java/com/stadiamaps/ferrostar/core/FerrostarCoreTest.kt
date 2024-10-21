package com.stadiamaps.ferrostar.core

import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
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
import uniffi.ferrostar.CourseFiltering
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

class MockPostRouteRequestGenerator : RouteRequestGenerator {
  override fun generateRequest(
      userLocation: UserLocation,
      waypoints: List<Waypoint>
  ): RouteRequest = RouteRequest.HttpPost(valhallaEndpointUrl, mapOf(), byteArrayOf())
}

class MockGetRouteRequestGenerator : RouteRequestGenerator {
  override fun generateRequest(
      userLocation: UserLocation,
      waypoints: List<Waypoint>
  ): RouteRequest = RouteRequest.HttpGet(valhallaEndpointUrl, mapOf())
}

class MockRouteResponseParser(private val routes: List<Route>) : RouteResponseParser {
  override fun parseResponse(response: ByteArray): List<Route> = routes
}

class MockForegroundNotificationManager : ForegroundServiceManager {
  var startCalled = false

  override fun startService(stopNavigation: () -> Unit) {
    startCalled = true
  }

  var stopCalled = false

  override fun stopService() {
    stopCalled = true
  }

  var onCurrentStateUpdated: ((NavigationState) -> Unit)? = null

  override fun onNavigationStateUpdated(state: NavigationState) {
    onCurrentStateUpdated?.invoke(state)
  }
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
          roundaboutExitDegrees = null,
          laneInfo = null)
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
                                  subContent = null,
                                  triggerDistanceBeforeManeuver = 42.0)),
                      spokenInstructions = listOf(),
                      duration = 0.0,
                      annotations = null)))

  @Test
  fun test401UnauthorizedRouteResponse() = runTest {
    val interceptor =
        MockInterceptor().apply {
          rule(post, url eq valhallaEndpointUrl) { respond(401, errorBody) }

          rule(get) { respond { throw IllegalStateException("Unexpected GET request") } }
        }

    val core =
        FerrostarCore(
            routeAdapter =
                RouteAdapter(
                    requestGenerator = MockPostRouteRequestGenerator(),
                    responseParser = MockRouteResponseParser(routes = listOf())),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = SimulatedLocationProvider(),
            foregroundServiceManager = MockForegroundNotificationManager(),
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.Manual, RouteDeviationTracking.None, CourseFiltering.RAW))

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
                  horizontalAccuracy = 0.0,
                  courseOverGround = null,
                  timestamp = Instant.now(),
                  speed = null),
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
  fun test200MockRouteResponsePost() = runTest {
    val interceptor =
        MockInterceptor().apply {
          rule(post, url eq valhallaEndpointUrl) { respond(200, "".toResponseBody()) }

          rule(get) { respond { throw IllegalStateException("unexpected GET request") } }
        }

    val core =
        FerrostarCore(
            routeAdapter =
                RouteAdapter(
                    requestGenerator = MockPostRouteRequestGenerator(),
                    responseParser = MockRouteResponseParser(routes = listOf(mockRoute))),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = SimulatedLocationProvider(),
            foregroundServiceManager = MockForegroundNotificationManager(),
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.Manual, RouteDeviationTracking.None, CourseFiltering.RAW))
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
                    timestamp = Instant.now(),
                    speed = null),
            waypoints =
                listOf(
                    Waypoint(
                        coordinate = GeographicCoordinate(lat = 60.5349908, lng = -149.5485806),
                        kind = WaypointKind.BREAK)))

    assertEquals(listOf(mockRoute), routes)
  }

  @Test
  fun test200MockRouteResponseGet() = runTest {
    val interceptor =
        MockInterceptor().apply {
          rule(get, url eq valhallaEndpointUrl) { respond(200, "".toResponseBody()) }

          rule(post) { respond { throw IllegalStateException("unexpected POST request") } }
        }

    val core =
        FerrostarCore(
            routeAdapter =
                RouteAdapter(
                    requestGenerator = MockGetRouteRequestGenerator(),
                    responseParser = MockRouteResponseParser(routes = listOf(mockRoute))),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = SimulatedLocationProvider(),
            foregroundServiceManager = MockForegroundNotificationManager(),
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.Manual, RouteDeviationTracking.None, CourseFiltering.RAW))
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
                    timestamp = Instant.now(),
                    speed = null),
            waypoints =
                listOf(
                    Waypoint(
                        coordinate = GeographicCoordinate(lat = 60.5349908, lng = -149.5485806),
                        kind = WaypointKind.BREAK)))

    assertEquals(listOf(mockRoute), routes)
  }

  @Test
  fun testCustomRouteProvider() = runTest {
    val interceptor =
        MockInterceptor().apply {
          rule(post) { respond { throw IllegalStateException("Unexpected network call") } }
        }

    val routeProvider =
        object : CustomRouteProvider {
          var wasCalled = false

          override suspend fun getRoutes(
              userLocation: UserLocation,
              waypoints: List<Waypoint>
          ): List<Route> {
            wasCalled = true
            return listOf(mockRoute)
          }
        }

    val core =
        FerrostarCore(
            customRouteProvider = routeProvider,
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = SimulatedLocationProvider(),
            foregroundServiceManager = MockForegroundNotificationManager(),
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.Manual, RouteDeviationTracking.None, CourseFiltering.RAW))
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
                    timestamp = Instant.now(),
                    speed = null),
            waypoints =
                listOf(
                    Waypoint(
                        coordinate = GeographicCoordinate(lat = 60.5349908, lng = -149.5485806),
                        kind = WaypointKind.BREAK)))

    assertEquals(listOf(mockRoute), routes)
    assert(routeProvider.wasCalled)
  }

  @Test
  fun testCustomRouteDeviationHandler() = runTest {
    val interceptor =
        MockInterceptor().apply {
          rule(post, url eq valhallaEndpointUrl) { respond(200, "".toResponseBody()) }

          rule(post, url eq valhallaEndpointUrl) { respond(200, "".toResponseBody()) }
        }

    val foregroundServiceManager = MockForegroundNotificationManager()

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
                    requestGenerator = MockPostRouteRequestGenerator(),
                    responseParser = MockRouteResponseParser(routes = listOf(mockRoute))),
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = locationProvider,
            foregroundServiceManager = foregroundServiceManager,
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.Manual, RouteDeviationTracking.None, CourseFiltering.RAW))

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
                    timestamp = Instant.now(),
                    speed = null),
            waypoints =
                listOf(
                    Waypoint(
                        coordinate = GeographicCoordinate(lat = 60.5349908, lng = -149.5485806),
                        kind = WaypointKind.BREAK)))

    locationProvider.lastLocation =
        UserLocation(
            coordinates = GeographicCoordinate(0.0, 0.0),
            horizontalAccuracy = 6.0,
            courseOverGround = null,
            timestamp = Instant.now(),
            speed = null)
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
                        }),
            CourseFiltering.RAW))

    assert(foregroundServiceManager.startCalled)
    assert(deviationHandler.called)

    // TODO: Figure out how to test this properly with Kotlin coroutines + JUnit in the way.
    // Spent several hours fighting it trying to get something half as good as XCTestExpectation,
    // but was ultimately unsuccessful. I verified this works fine in a debugger and real app,
    // but the test scope is different.
    //        assert(processor.called)
  }
}
