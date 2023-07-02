import XCTest
import CoreLocation
import FFI
@testable import FerrostarCore

private let backendUrl = URL(string: "https://api.stadiamaps.com/route/v1")!
let errorBody = """
{
    "error": "No valid authentication provided."
}
""".data(using: .utf8)
let errorResponse = HTTPURLResponse(url: backendUrl, statusCode: 401, httpVersion: "HTTP/1.1", headerFields: ["Content-Type": "application/json"])!

private class DummyRouteAdapter: RouteAdapterProtocol {
    private let routes: [Route]

    init(routes: [Route]) {
        self.routes = routes
    }

    func generateRequest(waypoints: [FFI.GeographicCoordinates]) throws -> FFI.RouteRequest {
        return FFI.RouteRequest.httpPost(url: backendUrl.absoluteString, headers: [:], body: [])
    }

    func parseResponse(response: [UInt8]) throws -> [FFI.Route] {
        return routes
    }
}

final class FerrostarCoreTests: XCTestCase {
    func test401UnauthorizedRouteResponse() {
        let exp = expectation(description: "We should receive a failure response")

        let mockSession = MockURLSession()
        mockSession.registerMock(forURL: backendUrl, withData: errorBody, andResponse: errorResponse)

        class CoreDelegate: FerrostarCoreDelegate {
            private let expectation: XCTestExpectation

            init(expectation: XCTestExpectation) {
                self.expectation = expectation
            }

            func core(_ core: FerrostarCore, didUpdateLocation snappedLocation: CLLocation, andHeading heading: CLHeading?) {
                // No-op
            }

            func core(_ core: FerrostarCore, locationManagerFailedWithError error: Error) {
                XCTFail(error.localizedDescription)
            }

            func core(_ core: FerrostarCore, foundRoutes routes: [FFI.Route]) {
                XCTFail("Expected the route request to fail")
            }

            func core(_ core: FerrostarCore, routingFailedWithError error: Error) {
                guard let error = error as? FerrostarCoreError else {
                    XCTFail("Expected FerrostarCoreError")
                    return
                }

                XCTAssertEqual(error, .HTTPStatusCode(401))

                expectation.fulfill()
            }

            func core(_ core: FerrostarCore, didUpdateNavigationState update: NavigationStateUpdate) {
                XCTFail("No state updates expected")
            }
        }

        let core = FerrostarCore(routeAdapter: DummyRouteAdapter(routes: []), locationManager: SimulatedLocationManager(), networkSession: mockSession)
        let delegate = CoreDelegate(expectation: exp)
        core.delegate = delegate

        core.getRoutes(waypoints: [CLLocationCoordinate2D(latitude: 60.5349908, longitude: -149.5485806)], initialLocation: CLLocation(latitude: 60.5347155, longitude: -149.543469))

        wait(for: [exp], timeout: 1.0)
    }

    // TODO: Various location services failure modes (need special mocks to simulate these)
}
