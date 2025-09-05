import FerrostarCore
import MapLibreSwiftUI

extension MapViewCamera {
    static func currentLocationCamera(locationProvider: LocationProviding) -> MapViewCamera {
        guard let coordinate = locationProvider.lastLocation?.clLocation.coordinate
        else { return MapViewCamera.default() }
        return .center(coordinate, zoom: 14)
    }
}
