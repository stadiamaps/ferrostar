import CoreLocation
import UniFFI

extension CLLocationCoordinate2D {
    var geographicCoordinates: UniFFI.GeographicCoordinate {
        UniFFI.GeographicCoordinate(lat: latitude, lng: longitude)
    }

    init(geographicCoordinates: GeographicCoordinate) {
        self.init(latitude: geographicCoordinates.lat, longitude: geographicCoordinates.lng)
    }
}

extension CLLocation {
    var userLocation: UniFFI.UserLocation {
        let ffiCourse: UniFFI.CourseOverGround?

        if course >= 0 && courseAccuracy >= 0 {
            ffiCourse = UniFFI.CourseOverGround(course: course, courseAccuracy: courseAccuracy)
        } else {
            ffiCourse = nil
        }

        return UniFFI.UserLocation(coordinates: coordinate.geographicCoordinates, horizontalAccuracy: horizontalAccuracy, courseOverGround: ffiCourse, timestamp: timestamp)
    }

    convenience init(userLocation: UniFFI.UserLocation) {
        let invalid: Double = -1.0

        let courseDegrees : CLLocationDirection
        let courseAccuracy: CLLocationDirectionAccuracy
        if let course = userLocation.courseOverGround {
            courseDegrees = CLLocationDirection(course.degrees)
            courseAccuracy = CLLocationDirectionAccuracy(course.accuracy)
        } else {
            courseDegrees = invalid
            courseAccuracy = invalid
        }

        self.init(coordinate: CLLocationCoordinate2D(geographicCoordinates: userLocation.coordinates), altitude: invalid, horizontalAccuracy: userLocation.horizontalAccuracy, verticalAccuracy: invalid, course: courseDegrees, courseAccuracy: courseAccuracy, speed: invalid, speedAccuracy: invalid, timestamp: userLocation.timestamp)
    }
}

// MARK: Ferrostar Models

extension GeographicCoordinate {
    
    public var clLocationCoordinate2D: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: lat,
                               longitude: lng)
    }
    
    public init(cl clLocationCoordinate2D: CLLocationCoordinate2D) {
        self.init(lat: clLocationCoordinate2D.latitude,
                  lng: clLocationCoordinate2D.longitude)
    }
}

extension CourseOverGround {
    
    /// Intialize a Course Over Ground object from the relevant Core Location types. These can be found on a
    /// CLLocation object.
    ///
    /// This returns nil if the course or courseAccuracy are invalid as defined by Apple (negative)
    ///
    /// - Parameters:
    ///   - course: The direction the device is travelling, measured in degrees relative to true north.
    ///   - courseAccuracy: The accuracy of the direction
    public init?(course: CLLocationDirection,
                 courseAccuracy: CLLocationDirectionAccuracy) {
        guard course >= 0 && courseAccuracy >= 0 else {
            return nil
        }
        
        self.init(degrees: UInt16(course), accuracy: UInt16(courseAccuracy))
    }
}

extension Heading {
    
    /// Initialize a Heading if it can be represented as integers
    ///
    /// This returns nil if the heading or accuracy  are invalid as defined by Apple (negative)
    ///
    /// - Parameter clHeading: The CoreLocation heading provided by the location manager.
    public init?(clHeading: CLHeading) {
        guard clHeading.trueHeading >= 0
            && clHeading.headingAccuracy >= 0 else {
            return nil
        }
        
        self.init(trueHeading: UInt16(clHeading.trueHeading),
                  accuracy: UInt16(clHeading.headingAccuracy),
                  timestamp: clHeading.timestamp)
    }
}

extension UserLocation {
    
    /// Initialize a UserLocation from values.
    ///
    /// - Parameters:
    ///   - latitude: The latitude in decimal degrees
    ///   - longitude: The longitude in decimal degrees
    ///   - horizontalAccuracy: The horizontal accuracy
    ///   - course: The direction of travel measured in degrees clockwise from north.
    ///   - courseAccuracy: The course accuracy measured in degrees.
    ///   - timestamp: The timestamp of the location record.
    public init(latitude: CLLocationDegrees,
                longitude: CLLocationDegrees,
                horizontalAccuracy: CLLocationDistance,
                course: CLLocationDirection,
                courseAccuracy: CLLocationDirectionAccuracy,
                timestamp: Date) {
        
        self.init(coordinates: GeographicCoordinate(lat: latitude, lng: longitude),
                  horizontalAccuracy: horizontalAccuracy,
                  courseOverGround: CourseOverGround(course: course, courseAccuracy: courseAccuracy),
                  timestamp: timestamp)
    }
    
    /// Initialize a UserLocation with a coordinate only.
    ///
    /// - Parameter clCoordinateLocation2D: A core location coordinate.
    public init(clCoordinateLocation2D: CLLocationCoordinate2D) {
        // This behavior matches how CLLocation initializes with a coordinate (setting accuracy to 0 & date to now)
        self.init(coordinates: GeographicCoordinate(cl: clCoordinateLocation2D),
                  horizontalAccuracy: 0,
                  courseOverGround: nil,
                  timestamp: Date())
    }
    
    /// Initialize a UserLocation from an Apple CoreLocation CLLocation
    ///
    /// Unlike CourseOverGround & Heading, this value will init with invalid values.
    ///
    /// - Parameter clLocation: The location.
    public init(clLocation: CLLocation) {
        self.init(
            coordinates: GeographicCoordinate(cl: clLocation.coordinate),
            horizontalAccuracy: clLocation.horizontalAccuracy,
            courseOverGround: CourseOverGround(
                course: clLocation.course,
                courseAccuracy: clLocation.courseAccuracy),
            timestamp: clLocation.timestamp)
    }
}
