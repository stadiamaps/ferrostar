import Foundation
import MapLibreSwiftUI

public extension MapViewCamera {
    /// Is the camera currently tracking (navigating)
    var isTrackingUserLocationWithCourse: Bool {
        if case .trackingUserLocationWithCourse = state {
            return true
        }
        return false
    }

    /// The default camera configured for navigation.
    ///
    /// - Parameters:
    ///   - zoom: The zoom value (default is 18.0)
    ///   - pitch: The pitch (default is 45.0)
    /// - Returns: The configured MapViewCamera
    static func navigation(zoom: Double = 18.0, pitch: Double = 45.0) -> MapViewCamera {
        MapViewCamera.trackUserLocationWithCourse(zoom: zoom,
                                                  pitch: pitch)
    }
}
