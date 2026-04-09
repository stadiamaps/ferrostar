import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftUI

/// Computes the navigation camera button state from live map/tracking state.
///
/// This centralizes the follow/overview/manual decision logic used by the navigation views:
/// - hidden when not navigating
/// - recenter when the map is in route overview (`.rect`)
/// - current-location when user tracking is disengaged (`.none`)
/// - route-overview when actively following the user
struct NavigationCameraControlResolver {
    let isNavigating: Bool
    let camera: MapViewCamera
    let userTrackingMode: MLNUserTrackingMode
    let navigationCamera: MapViewCamera
    let routeOverviewCamera: MapViewCamera?

    let setCamera: (MapViewCamera) -> Void

    /// Build the camera button state and its action closure for the current map state.
    func cameraControlState() -> CameraControlState {
        guard isNavigating else {
            return .hidden
        }

        if isInOverviewMode {
            return .showRecenter(recenterToFollowMode)
        }

        if !isFollowingUser {
            return .showCurrentLocation(recenterToFollowMode)
        }

        guard let routeOverviewCamera else {
            return .hidden
        }

        return .showRouteOverview {
            setCamera(routeOverviewCamera)
        }
    }

    private var isInOverviewMode: Bool {
        if case .rect = camera.state {
            return true
        }
        return false
    }

    private var isFollowingUser: Bool {
        userTrackingMode != .none
    }

    private func recenterToFollowMode() {
        var followCamera = navigationCamera
        // We alternate `lastReasonForChange` here so repeated recenter taps
        // always produce a non-equal camera update and are not dropped as no-ops.
        followCamera.lastReasonForChange = camera.lastReasonForChange == .programmatic ? nil : .programmatic
        setCamera(followCamera)
    }
}
