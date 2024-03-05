import CoreLocation
import FerrostarCoreFFI
import XCTest
@testable import FerrostarCore

final class SimulatedLocationManagerTests: XCTestCase {
    func testInitialValuesAreNull() {
        let locationManager = SimulatedLocationProvider()
        XCTAssertNil(locationManager.lastLocation, "Initial location must be nil")
        XCTAssertNil(locationManager.lastHeading, "Initial heading must be nil")
    }

    func testSetLocation() {
        let locationManager = SimulatedLocationProvider()

        let location = CLLocation(latitude: 42, longitude: 24).userLocation
        locationManager.lastLocation = location

        XCTAssertEqual(locationManager.lastLocation, location)
    }

    func testSetHeading() {
        let locationManager = SimulatedLocationProvider()

        let heading = Heading(trueHeading: 42, accuracy: 0, timestamp: Date())
        locationManager.lastHeading = heading

        XCTAssertEqual(locationManager.lastHeading, heading)
    }

    func testDelegateSetLocation() {
        let exp = expectation(description: "The delegate should receive two location updates")
        exp.expectedFulfillmentCount = 2

        class LocationDelegate: LocationManagingDelegate {
            private let expectation: XCTestExpectation
            private var expectedLocations: [UserLocation]

            init(expectation: XCTestExpectation, expectedLocations: [UserLocation]) {
                self.expectation = expectation
                self.expectedLocations = expectedLocations
            }

            func locationManager(_: LocationProviding, didUpdateLocations locations: [UserLocation]) {
                XCTAssertEqual(locations.last, expectedLocations.removeFirst())
                expectation.fulfill()
            }

            func locationManager(_: LocationProviding, didUpdateHeading _: Heading) {
                XCTFail("Unexpected heading update")
            }

            func locationManager(_: LocationProviding, didFailWithError _: Error) {
                XCTFail("Unexpected failure")
            }
        }

        var locations = [
            CLLocation(latitude: 42, longitude: 24).userLocation,
            CLLocation(latitude: 24, longitude: 42).userLocation,
        ]

        let locationManager = SimulatedLocationProvider()
        locationManager.delegate = LocationDelegate(expectation: exp, expectedLocations: locations)
        locationManager.startUpdating()

        locationManager.lastLocation = locations.removeFirst()
        locationManager.lastLocation = locations.removeFirst()

        wait(for: [exp], timeout: 1.0)
    }

    func testDelegateSetHeading() {
        let exp = expectation(description: "The delegate should receive two heading updates")
        exp.expectedFulfillmentCount = 2

        class LocationDelegate: LocationManagingDelegate {
            private let expectation: XCTestExpectation
            private var expectedHeadings: [Heading]

            init(expectation: XCTestExpectation, expectedHeadings: [Heading]) {
                self.expectation = expectation
                self.expectedHeadings = expectedHeadings
            }

            func locationManager(_: LocationProviding, didUpdateLocations _: [UserLocation]) {
                XCTFail("Unexpected location update")
            }

            func locationManager(_: LocationProviding, didUpdateHeading newHeading: Heading) {
                XCTAssertEqual(newHeading, expectedHeadings.removeFirst())
                expectation.fulfill()
            }

            func locationManager(_: LocationProviding, didFailWithError _: Error) {
                XCTFail("Unexpected failure")
            }
        }

        var headings = [
            Heading(trueHeading: 42, accuracy: 0, timestamp: Date()),
            Heading(trueHeading: 24, accuracy: 0, timestamp: Date()),
        ]

        let locationManager = SimulatedLocationProvider()
        locationManager.delegate = LocationDelegate(expectation: exp, expectedHeadings: headings)
        locationManager.startUpdating()

        locationManager.lastHeading = headings.removeFirst()
        locationManager.lastHeading = headings.removeFirst()

        wait(for: [exp], timeout: 1.0)
    }
}
