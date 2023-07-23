import Foundation
import FerrostarCore
import Mapbox

extension FerrostarObservableState {
    var routePolyline: MGLPolyline {
        return MGLPolyline(coordinates: fullRoute, count: UInt(fullRoute.count))
    }

    var remainingRoutePolyline: MGLPolyline {
        return MGLPolyline(coordinates: remainingWaypoints, count: UInt(remainingWaypoints.count))
    }
}
