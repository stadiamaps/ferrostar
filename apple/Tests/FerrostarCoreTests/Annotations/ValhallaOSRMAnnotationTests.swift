import XCTest
@testable import FerrostarCore

final class ValhallaOSRMAnnotationTests: XCTestCase {

    let encoder = JSONEncoder()
    let decoder = JSONDecoder()

    func testEmptyAnnotations() throws {
        try assertAnnotations {
            ValhallaOSRMAnnotation(
                speedLimit: nil,
                speed: nil,
                distance: nil,
                duration: nil
            )
        }
    }
    
    func testMilesPerHour() throws {
        try assertAnnotations {
            ValhallaOSRMAnnotation(
                speedLimit: .speed(15, unit: .milesPerHour),
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }
    
    func testKilometersPerHour() throws {
        try assertAnnotations {
            ValhallaOSRMAnnotation(
                speedLimit: .speed(15, unit: .kilometersPerHour),
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }
    
    func testKnots() throws {
        try assertAnnotations {
            ValhallaOSRMAnnotation(
                speedLimit: .speed(15, unit: .knots),
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }
    
    func testNone() throws {
        try assertAnnotations {
            ValhallaOSRMAnnotation(
                speedLimit: MaxSpeed.none,
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }
    
    func testUnknown() throws {
        try assertAnnotations {
            ValhallaOSRMAnnotation(
                speedLimit: .unknown,
                speed: 11.0,
                distance: 12.0,
                duration: 13.0
            )
        }
    }
    
    func assertAnnotations(_ makeAnnotations: () -> ValhallaOSRMAnnotation) throws {
        let annotations = makeAnnotations()
        
        let encoded = try encoder.encode(annotations)
        let result = try decoder.decode(ValhallaOSRMAnnotation.self, from: encoded)
        
        XCTAssertEqual(annotations, result)
    }
}
