import CoreLocation
import UniFFI

extension CLLocationCoordinate2D {
    var geographicCoordinates: UniFFI.GeographicCoordinates {
        UniFFI.GeographicCoordinates(lat: latitude, lng: longitude)
    }

    init(geographicCoordinates: GeographicCoordinates) {
        self.init(latitude: geographicCoordinates.lat, longitude: geographicCoordinates.lng)
    }
}

extension CLLocation {
    var userLocation: UniFFI.UserLocation {
        let ffiCourse: UniFFI.CourseOverGround?

        if course >= 0 && courseAccuracy >= 0 {
            ffiCourse = UniFFI.CourseOverGround(degrees: UInt16(course), accuracy: UInt16(courseAccuracy))
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
