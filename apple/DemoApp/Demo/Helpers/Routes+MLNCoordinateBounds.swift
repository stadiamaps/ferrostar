import FerrostarCoreFFI
import MapLibre

extension [Route] {
    var boundingBox: MLNCoordinateBounds? {
        let allCoords = self.flatMap(\.geometry)
        let allLats = allCoords.map(\.lat)
        let allLngs = allCoords.map(\.lng)

        guard let minLat = allLats.min(),
              let minLng = allLngs.min(),
              let maxLat = allLats.max(),
              let maxLng = allLngs.max()
        else {
            return nil
        }

        return MLNCoordinateBounds(
            sw: .init(latitude: minLat, longitude: minLng),
            ne: .init(latitude: maxLat, longitude: maxLng)
        )
    }
}
