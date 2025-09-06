import FerrostarCoreFFI
import MapLibre

extension Route {
    var polyline: MLNPolyline {
        MLNPolylineFeature(coordinates: geometry.map(\.clLocationCoordinate2D))
    }
}
