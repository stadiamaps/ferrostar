import Foundation
import MapLibreSwiftUI

public enum NavigationActivity {
    case automotive
    case bicycle
    case pedestrian

    public var zoom: Double {
        switch self {
        case .automotive:
            16.0
        case .bicycle:
            18.0
        case .pedestrian:
            20.0
        }
    }

    public var pitch: Double {
        switch self {
        case .automotive, .bicycle:
            45.0
        case .pedestrian:
            10.0
        }
    }
}

public extension MapViewCamera {
    /// Is the camera currently tracking (navigating)
    var isTrackingUserLocationWithCourse: Bool {
        if case .trackingUserLocationWithCourse = state {
            return true
        }
        return false
    }

    /// The default camera for navigation based on activity type.
    ///
    /// - Parameter activity: The navigation activity profile.
    /// - Returns: The configured MapViewCamera
    static func navigation(activity: NavigationActivity = .automotive) -> MapViewCamera {
        MapViewCamera.trackUserLocationWithCourse(
            zoom: activity.zoom,
            pitch: activity.pitch,
            pitchRange: .fixed(activity.pitch)
        )
    }

    /// The default camera for automotive navigation.
    ///
    /// - Parameters:
    ///   - zoom: The zoom value (default is 18.0)
    ///   - pitch: The pitch (default is 45.0)
    /// - Returns: The configured MapViewCamera
    static func automotiveNavigation(
        zoom: Double = NavigationActivity.automotive.zoom,
        pitch: Double = NavigationActivity.automotive.pitch
    ) -> MapViewCamera {
        MapViewCamera.trackUserLocationWithCourse(zoom: zoom,
                                                  pitch: pitch,
                                                  pitchRange: .fixed(pitch))
    }

    /// The default camera for bicycle navigation.
    ///
    /// - Parameters:
    ///   - zoom: The zoom value (default is 18.0)
    ///   - pitch: The pitch (default is 45.0)
    /// - Returns: The configured MapViewCamera
    static func bicycleNavigation(
        zoom: Double = NavigationActivity.bicycle.zoom,
        pitch: Double = NavigationActivity.bicycle.pitch
    ) -> MapViewCamera {
        MapViewCamera.trackUserLocationWithCourse(zoom: zoom,
                                                  pitch: pitch,
                                                  pitchRange: .fixed(pitch))
    }

    /// The default camera for pedestrian navigation.
    ///
    /// - Parameters:
    ///   - zoom: The zoom value (default is 20.0)
    ///   - pitch: The pitch (default is 10.0)
    /// - Returns: The configured MapViewCamera
    static func pedestrianNavigation(
        zoom: Double = NavigationActivity.pedestrian.zoom,
        pitch: Double = NavigationActivity.pedestrian.pitch
    ) -> MapViewCamera {
        MapViewCamera.trackUserLocationWithCourse(zoom: zoom,
                                                  pitch: pitch,
                                                  pitchRange: .fixed(pitch))
    }
}
