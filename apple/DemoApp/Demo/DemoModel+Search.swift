import CoreLocation
import Foundation
import StadiaMaps
import StadiaMapsAutocompleteSearch
import SwiftUI

private extension Point {
    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: coordinates[1], longitude: coordinates[0])
    }
}

extension DemoModel {
    var lastLocation: CLLocation? {
        guard let lastCoordinate else { return nil }
        return CLLocation(latitude: lastCoordinate.latitude, longitude: lastCoordinate.longitude)
    }

    func updateDestination(to point: Point) {
        destination = point.coordinate
        chooseDestination()
    }

    var searchView: some View {
        AutocompleteSearch(apiKey: sharedAPIKeys.stadiaMapsAPIKey, userLocation: lastLocation) {
            guard let geometry = $0.geometry else { return }
            self.updateDestination(to: geometry)
        }
    }
}
