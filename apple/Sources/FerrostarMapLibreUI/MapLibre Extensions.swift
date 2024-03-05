import FerrostarCore
import Foundation
import MapLibre

extension NavigationState {
    var routePolyline: MLNPolyline {
        MLNPolylineFeature(coordinates: fullRouteShape.map(\.clLocationCoordinate2D))
    }

    var remainingRoutePolyline: MLNPolyline {
        // FIXME:
        MLNPolylineFeature(coordinates: fullRouteShape.map(\.clLocationCoordinate2D))
    }
}
