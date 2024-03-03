// Integration tests of the core using the Valhalla backend with mocked
// responses

import CoreLocation
@testable import FerrostarCore
import XCTest
import FerrostarCoreFFI
import SnapshotTesting

final class ValhallaCoreTests: XCTestCase {
    @MainActor
    func testValhallaRouteParsing() async throws {
        let mockSession = MockURLSession()
        mockSession.registerMock(forURL: valhallaEndpointUrl, withData: sampleRouteData, andResponse: successfulJSONResponse)

        let core = FerrostarCore(valhallaEndpointUrl: valhallaEndpointUrl, profile: "auto", locationProvider: SimulatedLocationProvider(), networkSession: mockSession)
        let routes = try await core.getRoutes(initialLocation: CLLocation(latitude: 60.5347155, longitude: -149.543469), waypoints: [Waypoint(coordinate: GeographicCoordinate(lat: 60.5349908, lng: -149.5485806), kind: .break)])

        assertSnapshot(of: routes, as: .dump)
    }
}
