import CoreLocation
@testable import FerrostarCore
import XCTest

private class MockHeading: CLHeading {
    let value: CLLocationDirection
    override var magneticHeading: CLLocationDirection {
        value
    }

    override var trueHeading: CLLocationDirection {
        value
    }

    override var headingAccuracy: CLLocationDirection {
        0
    }

    init(value: CLLocationDirection) {
        self.value = value

        super.init()
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("You aren't supposted to call this")
    }
}

final class SimulatedLocationManagerTests: XCTestCase {
    func testInitialValuesAreNull() {
        let locationManager = SimulatedLocationProvider()
        XCTAssertNil(locationManager.lastLocation, "Initial location must be nil")
        XCTAssertNil(locationManager.lastHeading, "Initial heading must be nil")
    }

    func testSetLocation() {
        let locationManager = SimulatedLocationProvider()

        let location = CLLocation(latitude: 42, longitude: 24)
        locationManager.lastLocation = location

        XCTAssertEqual(locationManager.lastLocation, location)
    }

    func testSetHeading() {
        let locationManager = SimulatedLocationProvider()

        let heading = MockHeading(value: 42)
        locationManager.lastHeading = heading

        XCTAssertEqual(locationManager.lastHeading, heading)
    }

    func testDelegateSetLocation() {
        let exp = expectation(description: "The delegate should receive two location updates")
        exp.expectedFulfillmentCount = 2

        class LocationDelegate: LocationManagingDelegate {
            private let expectation: XCTestExpectation
            private var expectedLocations: [CLLocation]

            init(expectation: XCTestExpectation, expectedLocations: [CLLocation]) {
                self.expectation = expectation
                self.expectedLocations = expectedLocations
            }

            func locationManager(_: LocationProviding, didUpdateLocations locations: [CLLocation]) {
                XCTAssertEqual(locations.last, expectedLocations.removeFirst())
                expectation.fulfill()
            }

            func locationManager(_: LocationProviding, didUpdateHeading _: CLHeading) {
                XCTFail("Unexpected heading update")
            }

            func locationManager(_: LocationProviding, didFailWithError _: Error) {
                XCTFail("Unexpected failure")
            }
        }

        var locations = [CLLocation(latitude: 42, longitude: 24), CLLocation(latitude: 24, longitude: 42)]

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
            private var expectedHeadings: [CLHeading]

            init(expectation: XCTestExpectation, expectedHeadings: [CLHeading]) {
                self.expectation = expectation
                self.expectedHeadings = expectedHeadings
            }

            func locationManager(_: LocationProviding, didUpdateLocations _: [CLLocation]) {
                XCTFail("Unexpected location update")
            }

            func locationManager(_: LocationProviding, didUpdateHeading newHeading: CLHeading) {
                XCTAssertEqual(newHeading, expectedHeadings.removeFirst())
                expectation.fulfill()
            }

            func locationManager(_: LocationProviding, didFailWithError _: Error) {
                XCTFail("Unexpected failure")
            }
        }

        var headings = [MockHeading(value: 42), MockHeading(value: 24)]

        let locationManager = SimulatedLocationProvider()
        locationManager.delegate = LocationDelegate(expectation: exp, expectedHeadings: headings)
        locationManager.startUpdating()

        locationManager.lastHeading = headings.removeFirst()
        locationManager.lastHeading = headings.removeFirst()

        wait(for: [exp], timeout: 1.0)
    }
}
