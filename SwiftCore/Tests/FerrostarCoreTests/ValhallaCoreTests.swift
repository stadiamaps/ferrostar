// Integration tests of the core using the Valhalla backend with mocked
// responses

import CoreLocation
@testable import FerrostarCore
import UniFFI
import XCTest

private let valhallaEndpointUrl = URL(string: "https://api.stadiamaps.com/route/v1")!
private let simpleRoute = Data("""
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
""".utf8)
private let successfulResponse = HTTPURLResponse(url: valhallaEndpointUrl, statusCode: 200, httpVersion: "HTTP/1.1", headerFields: ["Content-Type": "application/json"])!

final class ValhallaCoreTests: XCTestCase {
    func testValhallaRouteParsing() async throws {
        let mockSession = MockURLSession()
        mockSession.registerMock(forURL: valhallaEndpointUrl, withData: simpleRoute, andResponse: successfulResponse)

        let core = FerrostarCore(valhallaEndpointUrl: valhallaEndpointUrl, profile: "auto", locationManager: SimulatedLocationManager(), networkSession: mockSession)
        let routes = try await core.getRoutes(waypoints: [CLLocationCoordinate2D(latitude: 60.5349908, longitude: -149.5485806)], initialLocation: CLLocation(latitude: 60.5347155, longitude: -149.543469))

        XCTAssertEqual(routes.count, 1)

        // Test polyline decoding.
        let expectedGeometry = [
            CLLocationCoordinate2D(
                latitude: 60.534716, longitude: -149.543469
            ),
            CLLocationCoordinate2D(
                latitude: 60.534782, longitude: -149.543879
            ),
            CLLocationCoordinate2D(
                latitude: 60.534829, longitude: -149.544134
            ),
            CLLocationCoordinate2D(
                latitude: 60.534856, longitude: -149.5443
            ),
            CLLocationCoordinate2D(
                latitude: 60.534887, longitude: -149.544533
            ),
            CLLocationCoordinate2D(
                latitude: 60.534941, longitude: -149.544976
            ),
            CLLocationCoordinate2D(
                latitude: 60.534971, longitude: -149.545485
            ),
            CLLocationCoordinate2D(
                latitude: 60.535003, longitude: -149.546177
            ),
            CLLocationCoordinate2D(
                latitude: 60.535008, longitude: -149.546937
            ),
            CLLocationCoordinate2D(
                latitude: 60.534991, longitude: -149.548581
            ),
        ]
        XCTAssertEqual(routes.first!.geometry.count, expectedGeometry.count)

        // Can't compare as double is not equatable
        for (result, expected) in zip(routes.first!.geometry, expectedGeometry) {
            XCTAssertEqual(result.latitude, expected.latitude, accuracy: 0.000001)
            XCTAssertEqual(result.longitude, expected.longitude, accuracy: 0.000001)
        }
    }
}
