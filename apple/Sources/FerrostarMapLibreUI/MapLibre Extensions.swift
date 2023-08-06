import Foundation
import FerrostarCore
import Mapbox

extension FerrostarObservableState {
    var routePolyline: MGLPolyline {
        return MGLPolyline(coordinates: fullRouteShape, count: UInt(fullRouteShape.count))
    }

    var remainingRoutePolyline: MGLPolyline {
        return MGLPolyline(coordinates: remainingWaypoints, count: UInt(remainingWaypoints.count))
    }
}
