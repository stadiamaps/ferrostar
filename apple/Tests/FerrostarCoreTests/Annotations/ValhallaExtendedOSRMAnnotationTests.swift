import XCTest
@testable import FerrostarCore

final class ValhallaOSRMAnnotationTests: XCTestCase {
    let encoder = JSONEncoder()
    let decoder = JSONDecoder()

    func testEmptyAnnotations() throws {
        try assertAnnotations {
            ValhallaExtendedOSRMAnnotation(
                speedLimit: nil,
                speed: nil,
                distance: nil,
                duration: nil
            )
        }
    }

    func testMilesPerHour() throws {
        try assertAnnotations {
            ValhallaExtendedOSRMAnnotation(
                speedLimit: .speed(15, unit: .milesPerHour),
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }

    func testKilometersPerHour() throws {
        try assertAnnotations {
            ValhallaExtendedOSRMAnnotation(
                speedLimit: .speed(15, unit: .kilometersPerHour),
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }

    func testKnots() throws {
        try assertAnnotations {
            ValhallaExtendedOSRMAnnotation(
                speedLimit: .speed(15, unit: .knots),
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }

    func testNoLimit() throws {
        try assertAnnotations {
            ValhallaExtendedOSRMAnnotation(
                speedLimit: MaxSpeed.noLimit,
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }

    func testUnknown() throws {
        try assertAnnotations {
            ValhallaExtendedOSRMAnnotation(
                speedLimit: .unknown,
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }

    func test_decodeFromString_withMaxSpeed() throws {
        // Unsupported/incomplete congestion is in the json, but ignored by our `ValhallaExtendedOSRMAnnotation`
        // Codable.
        let jsonString =
            "{\"distance\":4.294596842089401,\"duration\":1,\"speed\":4.2,\"congestion\":\"low\",\"maxspeed\":{\"speed\":56,\"unit\":\"km/h\"}}"
        guard let jsonData = jsonString.data(using: .utf8) else {
            XCTFail("Could not convert string to data")
            return
        }

        let result = try decoder.decode(ValhallaExtendedOSRMAnnotation.self, from: jsonData)

        XCTAssertEqual(result.distance, 4.294596842089401)
        XCTAssertEqual(result.duration, 1.0)
        XCTAssertEqual(result.speed, 4.2)
        XCTAssertEqual(result.speedLimit, .speed(56.0, unit: .kilometersPerHour))
    }

    func test_decodeFromString_incompleteModel_unlimitedMaxSpeed() throws {
        let jsonString = "{\"maxspeed\":{\"none\":true}}"
        guard let jsonData = jsonString.data(using: .utf8) else {
            XCTFail("Could not convert string to data")
            return
        }

        let result = try decoder.decode(ValhallaExtendedOSRMAnnotation.self, from: jsonData)

        XCTAssertNil(result.distance)
        XCTAssertNil(result.duration)
        XCTAssertNil(result.speed)
        XCTAssertEqual(result.speedLimit, .noLimit)
    }

    func test_decodeFromString_unknownSpeed() throws {
        let jsonString = "{\"distance\":2,\"duration\":1,\"speed\":3,\"maxspeed\":{\"unknown\":true}}"
        guard let jsonData = jsonString.data(using: .utf8) else {
            XCTFail("Could not convert string to data")
            return
        }

        let result = try decoder.decode(ValhallaExtendedOSRMAnnotation.self, from: jsonData)

        XCTAssertEqual(result.distance, 2.0)
        XCTAssertEqual(result.duration, 1.0)
        XCTAssertEqual(result.speed, 3.0)
        XCTAssertEqual(result.speedLimit, .unknown)
    }

    func test_decodeFromString_nilSpeed() throws {
        let jsonString = "{\"distance\":2,\"duration\":1,\"speed\":3}"
        guard let jsonData = jsonString.data(using: .utf8) else {
            XCTFail("Could not convert string to data")
            return
        }

        let result = try decoder.decode(ValhallaExtendedOSRMAnnotation.self, from: jsonData)

        XCTAssertEqual(result.distance, 2.0)
        XCTAssertEqual(result.duration, 1.0)
        XCTAssertEqual(result.speed, 3.0)
        XCTAssertNil(result.speedLimit)
    }

    func assertAnnotations(_ makeAnnotations: () -> ValhallaExtendedOSRMAnnotation) throws {
        let annotations = makeAnnotations()

        let encoded = try encoder.encode(annotations)
        let result = try decoder.decode(ValhallaExtendedOSRMAnnotation.self, from: encoded)

        XCTAssertEqual(annotations, result)
    }
}
