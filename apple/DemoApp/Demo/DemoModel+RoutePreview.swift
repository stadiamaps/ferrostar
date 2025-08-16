import FerrostarMapLibreUI
import MapLibreSwiftDSL
import UIKit

/// The preview route style is green with a white casing.
private struct PreviewRouteStyle: RouteStyle {
    public let color: UIColor = .systemGreen
    public let casingColor: UIColor? = .white
    public let lineCap: LineCap = .round
    public let lineJoin: LineJoin = .round
    public init() { /* No def */ }
}

extension DemoModel {
    @MapViewContentBuilder var routePreview: [StyleLayerDefinition] {
        if let selectedRoutePolyline {
            RouteStyleLayer(
                polyline: selectedRoutePolyline,
                identifier: "preview-route-polyline",
                style: PreviewRouteStyle()
            )
        }
    }
}
