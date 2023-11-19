import CoreLocation
@testable import FerrostarCore
import UniFFI
import XCTest
import SnapshotTesting

let errorBody = Data("""
{
    "error": "No valid authentication provided."
}
""".utf8)
let errorResponse = HTTPURLResponse(url: valhallaEndpointUrl, statusCode: 401, httpVersion: "HTTP/1.1", headerFields: ["Content-Type": "application/json"])!

// Simple test to ensure that the extensibility with native code is working.


private class MockRouteRequestGenerator: RouteRequestGenerator {
    func generateRequest(userLocation: UniFFI.UserLocation, waypoints: [UniFFI.GeographicCoordinate]) throws -> UniFFI.RouteRequest {
        return UniFFI.RouteRequest.httpPost(url: valhallaEndpointUrl.absoluteString, headers: [:], body: Data())
    }
}

private class MockRouteResponseParser: RouteResponseParser {
    private let routes: [UniFFI.Route]

    init(routes: [UniFFI.Route]) {
        self.routes = routes
    }

    func parseResponse(response: Data) throws -> [UniFFI.Route] {
        return routes
    }
}


final class FerrostarCoreTests: XCTestCase {
    func test401UnauthorizedRouteResponse() async throws {
        let mockSession = MockURLSession()
        mockSession.registerMock(forURL: valhallaEndpointUrl, withData: errorBody, andResponse: errorResponse)

        let routeAdapter = RouteAdapter(requestGenerator: MockRouteRequestGenerator(), responseParser: MockRouteResponseParser(routes: []))

        let core = FerrostarCore(routeAdapter: routeAdapter, locationManager: SimulatedLocationProvider(), networkSession: mockSession)

        do {
            // Tests that the core generates a request and attempts to process it, but throws due to the mocked network layer
            _ = try await core.getRoutes(initialLocation: CLLocation(latitude: 60.5347155, longitude: -149.543469), waypoints: [CLLocationCoordinate2D(latitude: 60.5349908, longitude: -149.5485806)])
            XCTFail("Expected an error")
        } catch let FerrostarCoreError.httpStatusCode(statusCode) {
            XCTAssertEqual(statusCode, 401)
        }
    }

    @MainActor
    func test200MockRouteResponse() async throws {
        let mockSession = MockURLSession()
        mockSession.registerMock(forURL: valhallaEndpointUrl, withData: Data(), andResponse: successfulJSONResponse)

        let geom = [GeographicCoordinate(lng: 0, lat: 0), GeographicCoordinate(lng: 1, lat: 1)]
        let instructionContent = VisualInstructionContent(text: "Sail straight", maneuverType: .depart, maneuverModifier: .straight, roundaboutExitDegrees: nil)
        let mockRoute = UniFFI.Route(geometry: geom, distance: 1, waypoints: geom, steps: [RouteStep(geometry: geom, distance: 1, roadName: "foo road", instruction: "Sail straight", visualInstructions: [VisualInstruction(primaryContent: instructionContent, secondaryContent: nil, triggerDistanceBeforeManeuver: 42)], spokenInstructions: [])])

        let routeAdapter = RouteAdapter(requestGenerator: MockRouteRequestGenerator(), responseParser: MockRouteResponseParser(routes: [mockRoute]))

        let core = FerrostarCore(routeAdapter: routeAdapter, locationManager: SimulatedLocationProvider(), networkSession: mockSession)

        // Tests that the core generates a request and then the mocked parser returns the expected routes
        let routes = try await core.getRoutes(initialLocation: CLLocation(latitude: 60.5347155, longitude: -149.543469), waypoints: [CLLocationCoordinate2D(latitude: 60.5349908, longitude: -149.5485806)])
        assertSnapshot(of: routes, as: .dump)
    }

    // TODO: Various location services failure modes (need special mocks to simulate these)
}
