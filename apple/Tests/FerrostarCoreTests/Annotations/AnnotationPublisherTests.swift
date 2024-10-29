import Combine
import CoreLocation
import FerrostarCoreFFI
import SwiftUI
import XCTest
@testable import FerrostarCore

final class AnnotationPublisherTests: XCTestCase {
    @Published var fakeNavigationState: NavigationState?
    var cancellables = Set<AnyCancellable>()

    func testSpeedLimitConversion() throws {
        let annotation = AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM()
        annotation.configure($fakeNavigationState)

        let exp = expectation(description: "speed limit converted")
        annotation.$speedLimit
            .compactMap { $0 }
            .sink { speedLimit in
                XCTAssertEqual(speedLimit.value, 15.0)
                XCTAssertEqual(speedLimit.unit, .milesPerHour)

                exp.fulfill()
            }
            .store(in: &cancellables)

        let annotationJson = try encode(
            ValhallaExtendedOSRMAnnotation(
                speedLimit: .speed(15, unit: .milesPerHour),
                speed: nil,
                distance: nil,
                duration: nil
            )
        )

        fakeNavigationState = makeNavigationState(annotationJson)
        wait(for: [exp], timeout: 10)
    }

    func testInvalidJSON() throws {
        let exp = expectation(description: "json decoder error")

        let annotation = AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM { error in
            XCTAssert(error is DecodingError)
            exp.fulfill()
        }
        annotation.configure($fakeNavigationState)

        annotation.$speedLimit
            .compactMap { $0 }
            .sink { _ in
                XCTFail("Should never be a non-nil speed limit.")
            }
            .store(in: &cancellables)

        fakeNavigationState = makeNavigationState("broken-json")
        wait(for: [exp], timeout: 10)
    }

    func testUnhandledException() throws {
        let annotation = AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM()
        annotation.configure($fakeNavigationState)

        let exp = expectation(description: "speed limit converted")
        annotation.$speedLimit
            .compactMap { $0 }
            .sink { speedLimit in
                XCTAssertEqual(speedLimit.value, 15.0)
                XCTAssertEqual(speedLimit.unit, .kilometersPerHour)

                exp.fulfill()
            }
            .store(in: &cancellables)

        fakeNavigationState = makeNavigationState("broken-json")

        let annotationJson = try encode(
            ValhallaExtendedOSRMAnnotation(
                speedLimit: .unknown,
                speed: nil,
                distance: nil,
                duration: nil
            )
        )

        fakeNavigationState = makeNavigationState(annotationJson)

        let annotationJsonTwo = try encode(
            ValhallaExtendedOSRMAnnotation(
                speedLimit: .speed(15, unit: .kilometersPerHour),
                speed: nil,
                distance: nil,
                duration: nil
            )
        )

        fakeNavigationState = makeNavigationState(annotationJsonTwo)

        wait(for: [exp], timeout: 10)
    }

    // MARK: Helpers

    func makeNavigationState(_ annotation: String) -> NavigationState {
        NavigationState(
            tripState: .navigating(
                currentStepGeometryIndex: 1,
                snappedUserLocation: UserLocation(
                    clCoordinateLocation2D: CLLocationCoordinate2D(latitude: 0.0, longitude: 0.0)
                ),
                remainingSteps: [],
                remainingWaypoints: [],
                progress: TripProgress(
                    distanceToNextManeuver: 1.0,
                    distanceRemaining: 2.0,
                    durationRemaining: 3.0
                ),
                deviation: .noDeviation,
                visualInstruction: nil,
                spokenInstruction: nil,
                annotationJson: annotation
            ),
            routeGeometry: [],
            isCalculatingNewRoute: false
        )
    }

    func encode(_ annotation: ValhallaExtendedOSRMAnnotation) throws -> String {
        let data = try JSONEncoder().encode(annotation)
        return String(data: data, encoding: .utf8)!
    }
}
