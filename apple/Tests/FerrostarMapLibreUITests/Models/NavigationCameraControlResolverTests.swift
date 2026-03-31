import CoreLocation
import MapLibre
import MapLibreSwiftUI
import XCTest
@testable import FerrostarMapLibreUI

final class NavigationCameraControlResolverTests: XCTestCase {
    func test_CurrentLocationActionSetsProgrammaticReason_WhenCameraReasonIsNotProgrammatic() {
        let camera = MapViewCamera.center(CLLocationCoordinate2D(latitude: 47.0, longitude: 13.0), zoom: 12)
        let navigationCamera = MapViewCamera.automotiveNavigation()
        var updatedCamera: MapViewCamera?

        let resolver = NavigationCameraControlResolver(
            isNavigating: true,
            camera: camera,
            userTrackingMode: .none,
            navigationCamera: navigationCamera,
            routeOverviewCamera: nil,
            setCamera: { updatedCamera = $0 }
        )

        guard case let .showCurrentLocation(action) = resolver.cameraControlState() else {
            return XCTFail("Expected current-location camera control state")
        }

        action()

        XCTAssertNotNil(updatedCamera)
        XCTAssertEqual(updatedCamera?.lastReasonForChange, .programmatic)
    }

    func test_CurrentLocationActionClearsProgrammaticReason_WhenCameraReasonIsProgrammatic() {
        var camera = MapViewCamera.center(CLLocationCoordinate2D(latitude: 47.0, longitude: 13.0), zoom: 12)
        camera.lastReasonForChange = .programmatic
        let navigationCamera = MapViewCamera.automotiveNavigation()
        var updatedCamera: MapViewCamera?

        let resolver = NavigationCameraControlResolver(
            isNavigating: true,
            camera: camera,
            userTrackingMode: .none,
            navigationCamera: navigationCamera,
            routeOverviewCamera: nil,
            setCamera: { updatedCamera = $0 }
        )

        guard case let .showCurrentLocation(action) = resolver.cameraControlState() else {
            return XCTFail("Expected current-location camera control state")
        }

        action()

        XCTAssertNotNil(updatedCamera)
        XCTAssertNil(updatedCamera?.lastReasonForChange)
    }
}
