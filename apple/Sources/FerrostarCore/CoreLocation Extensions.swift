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
            ffiCourse = UniFFI.CourseOverGround(degrees: course, accuracy: courseAccuracy)
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

extension Heading {
    
    public init(clHeading: CLHeading) {
        self.init(geographicHeading: clHeading.trueHeading,
                  accuracy: clHeading.headingAccuracy,
                  x: clHeading.x,
                  y: clHeading.y,
                  z: clHeading.z,
                  timestamp: clHeading.timestamp)
    }
}

extension UserLocation {
    
    /// Initialize a UserLocation from an Apple CoreLocation CLLocation
    ///
    /// - Parameter clLocation: The location.
    public init(clLocation: CLLocation) {
        self.init(
            coordinates: GeographicCoordinate(cl: clLocation.coordinate),
            horizontalAccuracy: clLocation.horizontalAccuracy,
            courseOverGround: CourseOverGround(
                degrees: clLocation.course,
                accuracy: clLocation.courseAccuracy),
            timestamp: clLocation.timestamp)
    }
}



