import CoreLocation
import FerrostarCoreFFI
import SnapshotTesting
import XCTest
@testable import FerrostarCore

final class ValhallaCoreTests: XCTestCase {
    @MainActor
    func testValhallaRouteParsing() async throws {
        let mockSession = MockURLSession()
        mockSession.registerMock(
            forMethod: "POST",
            andURL: valhallaEndpointUrl,
            withData: sampleRouteData,
            andResponse: successfulJSONResponse
        )

        let core = try FerrostarCore(
            valhallaEndpointUrl: valhallaEndpointUrl,
            profile: "auto",
            locationProvider: SimulatedLocationProvider(),
            navigationControllerConfig: .init(
                stepAdvance: .manual,
                routeDeviationTracking: .none,
                snappedLocationCourseFiltering: .raw
            ),
            networkSession: mockSession
        )
        let routes = try await core.getRoutes(
            initialLocation: UserLocation(
                latitude: 60.5347155,
                longitude: -149.543469,
                horizontalAccuracy: 0,
                course: 0,
                courseAccuracy: 0,
                timestamp: Date(),
                speed: nil,
                speedAccuracy: nil
            ),
            waypoints: [Waypoint(coordinate: GeographicCoordinate(lat: 60.5349908, lng: -149.5485806), kind: .break)]
        )

        // Redact the annotations in each RouteStep for snapshot assertion.
        // TODO: Revamp this test once an annotations parsing strategy is chosen
        let final = routes.map { route in
            var route = route
            let newSteps = route.steps.map { step in
                var step = step
                step.annotations = nil
                return step
            }
            route.steps = newSteps
            return route
        }

        assertSnapshot(of: final, as: .dump)
    }
}
