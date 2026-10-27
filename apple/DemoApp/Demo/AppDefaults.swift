import CoreLocation
import Foundation

enum AppDefaults {
    // CI uses a separate simulator build so normal demos keep their routing and location settings.
    #if FERROSTAR_UI_EXERCISE
        static let isUIExercise = true
        static let initialLocation = CLLocation(latitude: 40.775707, longitude: -74.031614)
        static let sampleDestination = CLLocationCoordinate2D(latitude: 40.777985, longitude: -74.040048)
        static let routeEndpoint = "http://localhost:8765/route"
        static let mapStyleURL = URL(string: "https://tiles.openfreemap.org/styles/liberty")!
    #else
        static let isUIExercise = false
        static let sampleDestination = CLLocationCoordinate2D(latitude: 37.31910, longitude: -122.01018)
        static let initialLocation = CLLocation(latitude: 37.332726, longitude: -122.031790)
        static let routeEndpoint = "https://api.stadiamaps.com/route/v1?api_key=\(sharedAPIKeys.stadiaMapsAPIKey)"
        static let mapStyleURL =
            URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(sharedAPIKeys.stadiaMapsAPIKey)")!
    #endif
}
