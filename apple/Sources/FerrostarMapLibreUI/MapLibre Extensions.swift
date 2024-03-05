import FerrostarCore
import Foundation
import MapLibre

extension NavigationState {
    var routePolyline: MLNPolyline {
        MLNPolylineFeature(coordinates: fullRouteShape.map(\.clLocationCoordinate2D))
    }

    var remainingRoutePolyline: MLNPolyline {
        // FIXME:
<<<<<<< HEAD
        return MLNPolylineFeature(coordinates: fullRouteShape.map { $0.clLocationCoordinate2D })
=======
        MLNPolylineFeature(coordinates: fullRouteShape.map(\.clLocationCoordinate2D))
>>>>>>> 746c43483e74319176f21e1fe96b78c038215c0b
    }
}
