import CoreLocation
import FFI

extension CLLocationCoordinate2D {
    var geographicCoordinates: FFI.GeographicCoordinates {
        FFI.GeographicCoordinates(lat: latitude, lng: longitude)
    }
}

extension CLLocation {
    var userLocation: FFI.UserLocation {
        let ffiCourse: FFI.Course?

        if course >= 0 && courseAccuracy >= 0 {
            ffiCourse = FFI.Course(degrees: UInt16(course), accuracy: UInt16(courseAccuracy))
        } else {
            ffiCourse = nil
        }

        return FFI.UserLocation(coordinates: coordinate.geographicCoordinates, horizontalAccuracy: horizontalAccuracy, course: ffiCourse)
    }
}
