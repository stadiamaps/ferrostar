import CoreLocation
@testable import FerrostarCore
import UniFFI
import XCTest

private let backendUrl = URL(string: "https://api.stadiamaps.com/route/v1")!
let errorBody = Data("""
{
    "error": "No valid authentication provided."
}
""".utf8)
let errorResponse = HTTPURLResponse(url: backendUrl, statusCode: 401, httpVersion: "HTTP/1.1", headerFields: ["Content-Type": "application/json"])!

private class MockRouteAdapter: RouteAdapterProtocol {

    private let routes: [UniFFI.Route]

    init(routes: [UniFFI.Route]) {
        self.routes = routes
    }

    func generateRequest(userLocation: UniFFI.UserLocation, waypoints: [UniFFI.GeographicCoordinates]) throws -> UniFFI.RouteRequest {
        return UniFFI.RouteRequest.httpPost(url: backendUrl.absoluteString, headers: [:], body: [])
    }

    func parseResponse(response _: [UInt8]) throws -> [UniFFI.Route] {
        return routes
    }
}

final class FerrostarCoreTests: XCTestCase {
    func test401UnauthorizedRouteResponse() async throws {
        let mockSession = MockURLSession()
        mockSession.registerMock(forURL: backendUrl, withData: errorBody, andResponse: errorResponse)

        let core = FerrostarCore(routeAdapter: MockRouteAdapter(routes: []), locationManager: SimulatedLocationManager(), networkSession: mockSession)

        do {
            _ = try await core.getRoutes(waypoints: [CLLocationCoordinate2D(latitude: 60.5349908, longitude: -149.5485806)],
                                         initialLocation: CLLocation(latitude: 60.5347155, longitude: -149.543469))
            XCTFail("Expected an error")
        } catch let FerrostarCoreError.httpStatusCode(statusCode) {
            XCTAssertEqual(statusCode, 401)
        }
    }

    // TODO: Various location services failure modes (need special mocks to simulate these)
}
