import Foundation
import FerrostarCore
import Mapbox

extension FerrostarObservableState {
    var routePolyline: MGLPolyline {
        return MGLPolylineFeature(coordinates: fullRouteShape)
    }

    var remainingRoutePolyline: MGLPolyline {
        return MGLPolylineFeature(coordinates: remainingWaypoints)
    }
}
