import FerrostarCore
import MapLibre
import MapLibreSwiftUI

public extension NavigationState {
    /// A MapViewCamera representing the overview of the current route.
    var routeOverviewCamera: MapViewCamera? {
        guard let firstCoordinate = routeGeometry.first else {
            return nil
        }

        let initial = MLNCoordinateBounds(
            sw: firstCoordinate.clLocationCoordinate2D,
            ne: firstCoordinate.clLocationCoordinate2D
        )
        let bounds = routeGeometry.reduce(initial) { acc, coord in
            MLNCoordinateBounds(
                sw: CLLocationCoordinate2D(latitude: min(acc.sw.latitude, coord.lat), longitude: min(
                    acc.sw.longitude,
                    coord.lng
                )),
                ne: CLLocationCoordinate2D(
                    latitude: max(acc.ne.latitude, coord.lat),
                    longitude: max(acc.ne.longitude, coord.lng)
                )
            )
        }

        return MapViewCamera.boundingBox(bounds, edgePadding: UIEdgeInsets(top: 20, left: 100, bottom: 20, right: 100))
    }
}
