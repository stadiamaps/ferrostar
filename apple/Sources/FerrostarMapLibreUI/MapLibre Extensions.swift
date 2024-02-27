import Foundation
import FerrostarCore
import MapLibre

extension NavigationState {
    var routePolyline: MLNPolyline {
        return MLNPolylineFeature(coordinates: fullRouteShape.map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng) })
    }

    var remainingRoutePolyline: MLNPolyline {
        // FIXME
        return MLNPolylineFeature(coordinates: fullRouteShape.map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng) })
    }
}
