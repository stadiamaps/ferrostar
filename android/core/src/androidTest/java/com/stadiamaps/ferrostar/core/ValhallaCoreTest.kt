/**
 * FIXME: This file should move out of Android Tests ASAP. It only exists here because I haven't yet
 * figured out how to build and link the platform-native binaries via JNI just yet and this works.
 * See https://github.com/willir/cargo-ndk-android-gradle/issues/12.
 *
 * This solution is STUPIDLY INEFFICIENT and will probably contribute to global climate change since
 * an Android emulator uses like two whole CPU cores when idling.
 */
package com.stadiamaps.ferrostar.core

import java.net.URL
import java.time.Instant
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mock.MediaTypes.MEDIATYPE_JSON
import okhttp3.mock.MockInterceptor
import okhttp3.mock.eq
import okhttp3.mock.get
import okhttp3.mock.post
import okhttp3.mock.respond
import okhttp3.mock.rule
import okhttp3.mock.url
import org.junit.Assert.assertEquals
import org.junit.Test
import uniffi.ferrostar.CourseFiltering
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.StepAdvanceMode
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind

const val simpleRoute =
    """
{
  "routes": [
    {
      "weight_name": "auto",
      "weight": 56.002,
      "duration": 11.488,
      "distance": 284,
      "legs": [
        {
          "via_waypoints": [],
          "annotation": {
            "maxspeed": [
              {
                "speed": 89,
                "unit": "km/h"
              },
              {
                "speed": 89,
                "unit": "km/h"
              },
              {
                "speed": 89,
                "unit": "km/h"
              },
              {
                "speed": 89,
                "unit": "km/h"
              },
              {
                "speed": 89,
                "unit": "km/h"
              },
              {
                "speed": 89,
                "unit": "km/h"
              },
              {
                "speed": 89,
                "unit": "km/h"
              },
              {
                "speed": 89,
                "unit": "km/h"
              },
              {
                "speed": 89,
                "unit": "km/h"
              }
            ],
            "speed": [
              24.7,
              24.7,
              24.7,
              24.7,
              24.7,
              24.7,
              24.7,
              24.7,
              24.7
            ],
            "distance": [
              23.6,
              14.9,
              9.6,
              13.2,
              25,
              28.1,
              38.1,
              41.6,
              90
            ],
            "duration": [
              0.956,
              0.603,
              0.387,
              0.535,
              1.011,
              1.135,
              1.539,
              1.683,
              3.641
            ]
          },
          "admins": [
            {
              "iso_3166_1_alpha3": "USA",
              "iso_3166_1": "US"
            }
          ],
          "weight": 56.002,
          "duration": 11.488,
          "steps": [
            {
              "intersections": [
                {
                  "bearings": [
                    288
                  ],
                  "entry": [
                    true
                  ],
                  "admin_index": 0,
                  "out": 0,
                  "geometry_index": 0,
                  "location": [
                    -149.543469,
                    60.534716
                  ]
                }
              ],
              "speedLimitUnit": "mph",
              "maneuver": {
                "type": "depart",
                "instruction": "Drive west on AK 1/Seward Highway.",
                "bearing_after": 288,
                "bearing_before": 0,
                "location": [
                  -149.543469,
                  60.534716
                ]
              },
              "speedLimitSign": "mutcd",
              "name": "Seward Highway",
              "duration": 11.488,
              "distance": 284,
              "driving_side": "right",
              "weight": 56.002,
              "mode": "driving",
              "ref": "AK 1",
              "geometry": "wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB"
            },
            {
              "intersections": [
                {
                  "bearings": [
                    89
                  ],
                  "entry": [
                    true
                  ],
                  "in": 0,
                  "admin_index": 0,
                  "geometry_index": 9,
                  "location": [
                    -149.548581,
                    60.534991
                  ]
                }
              ],
              "speedLimitUnit": "mph",
              "maneuver": {
                "type": "arrive",
                "instruction": "You have arrived at your destination.",
                "bearing_after": 0,
                "bearing_before": 269,
                "location": [
                  -149.548581,
                  60.534991
                ]
              },
              "speedLimitSign": "mutcd",
              "name": "Seward Highway",
              "duration": 0,
              "distance": 0,
              "driving_side": "right",
              "weight": 0,
              "mode": "driving",
              "ref": "AK 1",
              "geometry": "}kwmrBhavf|G??"
            }
          ],
          "distance": 284,
          "summary": "AK 1"
        }
      ],
      "geometry": "wzvmrBxalf|GcCrX}A|Nu@jI}@pMkBtZ{@x^_Afj@Inn@`@veB"
    }
  ],
  "waypoints": [
    {
      "distance": 0,
      "name": "AK 1",
      "location": [
        -149.543469,
        60.534715
      ]
    },
    {
      "distance": 0,
      "name": "AK 1",
      "location": [
        -149.548581,
        60.534991
      ]
    }
  ],
  "code": "Ok"
}
"""

class ValhallaCoreTest {
  private val valhallaEndpointUrl = "https://api.stadiamaps.com/navigate/v1"

  @Test
  fun parseValhallaRouteResponse(): TestResult {
    val interceptor =
        MockInterceptor().apply {
          rule(post, url eq valhallaEndpointUrl) { respond(simpleRoute, MEDIATYPE_JSON) }

          rule(get) { respond { throw IllegalStateException("Expected only one request") } }
        }
    val core =
        FerrostarCore(
            valhallaEndpointURL = URL(valhallaEndpointUrl),
            profile = "auto",
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = SimulatedLocationProvider(),
            foregroundServiceManager = MockForegroundNotificationManager(),
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.Manual, RouteDeviationTracking.None, CourseFiltering.RAW))

    return runTest {
      val routes =
          core.getRoutes(
              UserLocation(
                  GeographicCoordinate(60.5347155, -149.543469), 12.0, null, Instant.now(), null),
              waypoints =
                  listOf(
                      Waypoint(
                          coordinate = GeographicCoordinate(60.5349908, -149.5485806),
                          kind = WaypointKind.BREAK)))

      assertEquals(routes.count(), 1)
      assertEquals(
          listOf(
              GeographicCoordinate(60.534716, -149.543469),
              GeographicCoordinate(60.534782, -149.543879),
              GeographicCoordinate(60.534829, -149.544134),
              GeographicCoordinate(60.534856, -149.5443),
              GeographicCoordinate(60.534887, -149.544533),
              GeographicCoordinate(60.534941, -149.544976),
              GeographicCoordinate(60.534971, -149.545485),
              GeographicCoordinate(60.535003, -149.546177),
              GeographicCoordinate(60.535008, -149.546937),
              GeographicCoordinate(60.534991, -149.548581),
          ),
          routes.first().geometry)
    }
  }

  @Test
  fun valhallaRequestWithCostingOptions(): TestResult {
    val interceptor =
        MockInterceptor().apply {
          rule(post, url eq valhallaEndpointUrl) { respond(simpleRoute, MEDIATYPE_JSON) }

          rule(get) { respond { throw IllegalStateException("Expected only one request") } }
        }
    val core =
        FerrostarCore(
            valhallaEndpointURL = URL(valhallaEndpointUrl),
            profile = "auto",
            httpClient = OkHttpClient.Builder().addInterceptor(interceptor).build(),
            locationProvider = SimulatedLocationProvider(),
            foregroundServiceManager = MockForegroundNotificationManager(),
            navigationControllerConfig =
                NavigationControllerConfig(
                    StepAdvanceMode.Manual, RouteDeviationTracking.None, CourseFiltering.RAW),
            costingOptions = mapOf("auto" to mapOf("useTolls" to 0)))

    return runTest {
      val routes =
          core.getRoutes(
              UserLocation(
                  GeographicCoordinate(60.5347155, -149.543469), 12.0, null, Instant.now(), null),
              waypoints =
                  listOf(
                      Waypoint(
                          coordinate = GeographicCoordinate(60.5349908, -149.5485806),
                          kind = WaypointKind.BREAK)))

      assertEquals(routes.count(), 1)
    }
  }
}
