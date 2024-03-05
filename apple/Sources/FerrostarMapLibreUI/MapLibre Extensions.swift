import FerrostarCore
import Foundation
import MapLibre

extension NavigationState {
    var routePolyline: MLNPolyline {
        return MLNPolylineFeature(coordinates: fullRouteShape.map { $0.clLocationCoordinate2D })
    }

    var remainingRoutePolyline: MLNPolyline {
        // FIXME:
        return MLNPolylineFeature(coordinates: fullRouteShape.map { $0.clLocationCoordinate2D })
    }
}
