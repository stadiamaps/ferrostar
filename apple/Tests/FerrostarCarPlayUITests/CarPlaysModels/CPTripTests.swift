import CarPlay
import FerrostarCoreFFI
import FerrostarSwiftUI
import MapKit
import Testing
import TestSupport

@testable import FerrostarCarPlayUI

struct CPTripTests {
    @Test("CPTrip creation")
    func cpTripCreation() async throws {
        let route = Route(
            geometry: [],
            bbox: .init(sw: .init(lat: 0, lng: 0), ne: .init(lat: 1, lng: 1)),
            distance: 123.4,
            waypoints: [
                Waypoint(coordinate: .init(lat: 0.1, lng: 0.2), kind: .break),
                Waypoint(coordinate: .init(lat: 0.3, lng: 0.4), kind: .break),
            ],
            steps: []
        )

        let trip = try CPTrip.fromFerrostar(
            routes: [route],
            distanceFormatter: usaDistanceFormatter,
            durationFormatter: DefaultFormatters.durationFormat
        )

        #expect(trip.origin.placemark.coordinate.latitude == 0.1)
        #expect(trip.origin.placemark.coordinate.longitude == 0.2)

        #expect(trip.destination.placemark.coordinate.latitude == 0.3)
        #expect(trip.destination.placemark.coordinate.longitude == 0.4)

        #expect(trip.routeChoices.first?.summaryVariants.first == "Route 1")
        #expect(trip.routeChoices.first?.additionalInformationVariants?.first == "400 ft")
        #expect(trip.routeChoices.first?.selectionSummaryVariants?.first == "Selected Route 1")
    }
}
