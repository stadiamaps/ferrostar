import SwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import FerrostarCore

extension MapView {
    
    public func mapOverlayRoute(_ route: Route?,
                                style: any RouteStyle = ActiveRouteStyle(),
                                identifierPrefix: String = "route-polyline") -> MapView {
        
        guard let route else {
            return self
        }
        
        let polyline = MLNPolyline(coordinates: route.geometry)
        
        // TODO: Update if the source is already in the `self.userLayers`
        let existingIdentifiers = self.userLayers.map { $0.identifier }
        
        print("Existing \(existingIdentifiers)")
        
        let routePolylineSource = ShapeSource(identifier: "\(identifierPrefix)-source") {
            polyline
        }
        
        let newLayers: [StyleLayerDefinition] = [
            LineStyleLayer(identifier: "\(identifierPrefix)-casing", source: routePolylineSource)
                .lineCap(constant: style.lineCap)
                .lineJoin(constant: style.lineJoin)
                .lineColor(constant: style.casingColor)
                .lineWidth(interpolatedBy: .zoomLevel,
                           curveType: .exponential,
                           parameters: NSExpression(forConstantValue: 1.5),
                           stops: NSExpression(forConstantValue: [14: 6, 18: 24])),

            LineStyleLayer(identifier: identifierPrefix, source: routePolylineSource)
                .lineCap(constant: style.lineCap)
                .lineJoin(constant: style.lineJoin)
                .lineColor(constant: style.color)
                .lineWidth(interpolatedBy: .zoomLevel,
                           curveType: .exponential,
                           parameters: NSExpression(forConstantValue: 1.5),
                           stops: NSExpression(forConstantValue: [14: 3, 18: 16]))
        ]
        
        switch self.styleSource {
    
        case .url(let styleUrl):
            return MapView(styleURL: styleUrl,
                           camera: self.camera) {
                return self.userLayers + newLayers
            }
        }
    }
}
