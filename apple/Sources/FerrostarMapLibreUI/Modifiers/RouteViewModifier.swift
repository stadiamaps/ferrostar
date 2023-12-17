//
//  File.swift
//  
//
//  Created by Jacob Fielding on 12/16/23.
//

import SwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI

extension MapView {
    
    public func routePolyline(_ polyline: MLNPolyline?) -> MapView {
        guard let polyline else {
            return self
        }
        
        let routePolylineSource = ShapeSource(identifier: "route-polyline-source") {
            polyline
        }
        
        let layers: [StyleLayerDefinition] = [
            // TODO: Make this configurable via a modifier
            LineStyleLayer(identifier: "route-polyline-casing", source: routePolylineSource)
                .lineCap(constant: .round)
                .lineJoin(constant: .round)
                .lineColor(constant: .white)
                .lineWidth(interpolatedBy: .zoomLevel,
                           curveType: .exponential,
                           parameters: NSExpression(forConstantValue: 1.5),
                           stops: NSExpression(forConstantValue: [14: 6, 18: 24])),

            // TODO: Make this configurable via a modifier
            LineStyleLayer(identifier: "route-polyline", source: routePolylineSource)
                .lineCap(constant: .round)
                .lineJoin(constant: .round)
                .lineColor(constant: .lightGray)
                .lineWidth(interpolatedBy: .zoomLevel,
                           curveType: .exponential,
                           parameters: NSExpression(forConstantValue: 1.5),
                           stops: NSExpression(forConstantValue: [14: 3, 18: 16]))
        ]
        
        switch self.styleSource {
    
        case .url(let styleUrl):
            return MapView(styleURL: styleUrl,
                           camera: self.camera) {
                return layers
            }
        }
    }
}
