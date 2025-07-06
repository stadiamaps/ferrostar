import CoreLocation
import Foundation

enum AppDefaults {
    static let initialLocation = CLLocation(latitude: 37.332726, longitude: -122.031790)
    static let mapStyleURL =
        URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(sharedAPIKeys.stadiaMapsAPIKey)")!
}
