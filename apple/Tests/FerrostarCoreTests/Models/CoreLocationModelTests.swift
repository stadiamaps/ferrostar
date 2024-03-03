import XCTest
import CoreLocation
import UniFFI
@testable import FerrostarCore

class MockCLHeading: CLHeading {
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    init(trueHeading: CLLocationDirection,
         headingAccuracy: CLLocationDirectionAccuracy,
         timestamp: Date) {
        
        self._trueHeading = trueHeading
        self._headingAccuracy = headingAccuracy
        self._timestamp = timestamp
        super.init()
    }
    
    let _trueHeading: CLLocationDirection
    override var trueHeading: CLLocationDirection {
        _trueHeading
    }
    
    let _headingAccuracy: CLLocationDirectionAccuracy
    override var headingAccuracy: CLLocationDirection {
        _headingAccuracy
    }
    
    let _timestamp: Date
    override var timestamp: Date {
        _timestamp
    }
}

final class CoreLocationModelTests: XCTestCase {
    
    // MARK: UserLocation
    
    func testInitUserLocation() {
        let timestamp = Date()
        let coordinate = CLLocationCoordinate2D(latitude: -77.846323, longitude: 166.668235)
        let location = CLLocation(coordinate: coordinate,
                                  altitude: 10,
                                  horizontalAccuracy: 1.1,
                                  verticalAccuracy: 6.6,
                                  course: 45.5,
                                  courseAccuracy: 3.3,
                                  speed: 4.4,
                                  speedAccuracy: 1.1,
                                  timestamp: timestamp)
        
        let userLocation = UserLocation(clLocation: location)
        
        XCTAssertEqual(userLocation.coordinates.lat, -77.846323)
        XCTAssertEqual(userLocation.coordinates.lng, 166.668235)
        XCTAssertEqual(userLocation.horizontalAccuracy, 1.1)
        XCTAssertEqual(userLocation.courseOverGround?.degrees, 45)
        XCTAssertEqual(userLocation.courseOverGround?.accuracy, 3)
        XCTAssertEqual(userLocation.timestamp, timestamp)
    }
    
    // MARK: CourseOverGround
    
    func testInitCourseOverGround() {
        let valid = CourseOverGround(course: 90.6, courseAccuracy: 2.1)
        XCTAssertEqual(valid?.degrees, 90)
        XCTAssertEqual(valid?.accuracy, 2)
    }
    
    func testInvalidInitCourseOverGround() {
        let invalid = CourseOverGround(course: -1, courseAccuracy: -1)
        XCTAssertNil(invalid)
    }
    
    // MARK: Heading
    
    func testInitHeading() {
        let timestamp = Date()
        let mockHeading = MockCLHeading(trueHeading: 67.7,
                                        headingAccuracy: 8.8,
                                        timestamp: timestamp)
        
        let valid = Heading(clHeading: mockHeading)
        XCTAssertEqual(valid?.trueHeading, 67)
        XCTAssertEqual(valid?.accuracy, 8)
        XCTAssertEqual(valid?.timestamp, timestamp)
    }
    
    func testInvalidInitHeading() {
        let timestamp = Date()
        let mockHeading = MockCLHeading(trueHeading: -1,
                                        headingAccuracy: -1,
                                        timestamp: timestamp)
        
        let invalid = Heading(clHeading: mockHeading)
        XCTAssertNil(invalid)
    }
}
