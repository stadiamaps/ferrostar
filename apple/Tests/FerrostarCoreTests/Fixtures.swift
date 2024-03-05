import Foundation

let valhallaEndpointUrl = URL(string: "https://api.stadiamaps.com/navigate/v1")!
let successfulJSONResponse = HTTPURLResponse(
    url: valhallaEndpointUrl,
    statusCode: 200,
    httpVersion: "HTTP/1.1",
    headerFields: ["Content-Type": "application/json"]
)!

let sampleRouteData = Data("""
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
