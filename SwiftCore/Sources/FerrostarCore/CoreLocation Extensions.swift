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
        let ffiCourse: UniFFI.Course?

        if course >= 0 && courseAccuracy >= 0 {
            ffiCourse = UniFFI.Course(degrees: UInt16(course), accuracy: UInt16(courseAccuracy))
        } else {
            ffiCourse = nil
        }

        return UniFFI.UserLocation(coordinates: coordinate.geographicCoordinates, horizontalAccuracy: horizontalAccuracy, course: ffiCourse)
    }
}
