import UIKit
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI

/// A customizable style for the route and it's casing.
public protocol RouteStyle {
    var color: UIColor { get }
    var casingColor: UIColor? { get }
    // TODO: Add route & route casing scaling/diameter?
    var lineCap: LineCap { get }
    var lineJoin: LineJoin { get }
}

/// The active route style is blue with a white casing.
public struct ActiveRouteStyle: RouteStyle {
    public let color: UIColor = .systemBlue
    public let casingColor: UIColor? = .white
    public let lineCap: LineCap = .round
    public let lineJoin: LineJoin = .round
    public init() { /* No def */ }
}

/// The travelled route style is grey without a casing.
///
/// This is typically overlayed on top of the active route style of the entire polyline.
public struct TravelledRouteStyle: RouteStyle {
    public var color: UIColor = .systemGray
    public var casingColor: UIColor? = nil
    public let lineCap: LineCap = .round
    public let lineJoin: LineJoin = .round
    public init() { /* No def */ }
}

public struct RouteStyleLayer: StyleLayerCollection {
    
    private let polyline: MLNPolyline
    private let identifier: String
    private let style: RouteStyle
    
    /// Create a navigation route polyline layer
    ///
    /// - Parameters:
    ///   - polyline: The polyline representation of the layer
    ///   - identifier: The source and layer identifier prefix.
    ///   - style: The style of the route polyline. This will include whether a casing is included.
    init(polyline: MLNPolyline, identifier: String, style: RouteStyle = ActiveRouteStyle()) {
        self.polyline = polyline
        self.identifier = identifier
        self.style = style
    }
    
    public var layers: [StyleLayerDefinition] {
        let source = ShapeSource(identifier: "\(identifier)-source") {
            polyline
        }
        
        if let casingColor = style.casingColor {
            LineStyleLayer(identifier: "\(identifier)-casing", source: source)
                .lineCap(constant: style.lineCap)
                .lineJoin(constant: style.lineJoin)
                .lineColor(constant: casingColor)
                .lineWidth(interpolatedBy: .zoomLevel,
                           curveType: .exponential,
                           parameters: NSExpression(forConstantValue: 1.5),
                           stops: NSExpression(forConstantValue: [14: 6, 18: 24]))
        }

        LineStyleLayer(identifier: "\(identifier)-polyline", source: source)
            .lineCap(constant: style.lineCap)
            .lineJoin(constant: style.lineJoin)
            .lineColor(constant: style.color)
            .lineWidth(interpolatedBy: .zoomLevel,
                       curveType: .exponential,
                       parameters: NSExpression(forConstantValue: 1.5),
                       stops: NSExpression(forConstantValue: [14: 3, 18: 16]))
    }
}
