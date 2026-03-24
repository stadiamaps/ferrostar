import FerrostarMapLibreUI
import MapLibreSwiftUI
import XCTest

final class MapViewCameraActivityTests: XCTestCase {
    func testActivityNavigationCamerasTrackUserLocationWithCourse() {
        XCTAssertTrue(MapViewCamera.navigation(activity: .automotive).isTrackingUserLocationWithCourse)
        XCTAssertTrue(MapViewCamera.navigation(activity: .bicycle).isTrackingUserLocationWithCourse)
        XCTAssertTrue(MapViewCamera.navigation(activity: .pedestrian).isTrackingUserLocationWithCourse)
    }

    func testNamedNavigationCameraFactoriesTrackUserLocationWithCourse() {
        XCTAssertTrue(MapViewCamera.automotiveNavigation().isTrackingUserLocationWithCourse)
        XCTAssertTrue(MapViewCamera.bicycleNavigation().isTrackingUserLocationWithCourse)
        XCTAssertTrue(MapViewCamera.pedestrianNavigation().isTrackingUserLocationWithCourse)
    }
}
