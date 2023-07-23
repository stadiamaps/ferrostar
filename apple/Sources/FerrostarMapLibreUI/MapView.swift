import SwiftUI
import Mapbox
import FerrostarCore

public enum LayerPosition {
    case above(layerId: String)
    case below(layerId: String)
    case onTopOfOthers
}

public struct InitialLayerConfiguration {
    let position: LayerPosition
    let applyStyle: ((MGLLineStyleLayer) -> Void)?
}


struct MapView: UIViewRepresentable {
    // TODO: User location + puck or w/e
    // TODO: Viewport

    let styleURL: URL
    let routePolyline: MGLPolyline
    let remainingRoutePolyline: MGLPolyline


    /// Inital layer configuration. Note that changing this during the view lifecycle generally has no effect.
    let routeCasingInitialLayerConfig: InitialLayerConfiguration
    /// Inital layer configuration. Note that changing this during the view lifecycle generally has no effect.
    let routeInitialLayerConfig: InitialLayerConfiguration
    /// Inital layer configuration. Note that changing this during the view lifecycle generally has no effect.
    let remainingRouteInitialLayerConfig: InitialLayerConfiguration

    class Coordinator: NSObject, MGLMapViewDelegate {
        private var routeSource: MGLShapeSource?
        private var remainingRouteSource: MGLShapeSource?

        private let routeCasingInitialLayerConfig: InitialLayerConfiguration
        private let routeInitialLayerConfig: InitialLayerConfiguration
        private let remainingRouteInitialLayerConfig: InitialLayerConfiguration

        var routePolyline: MGLPolyline {
            didSet {
                routeSource?.shape = routePolyline
            }
        }

        var remainingRoutePolyline: MGLPolyline {
            didSet {
                remainingRouteSource?.shape = remainingRoutePolyline
            }
        }

        init(routePolyline: MGLPolyline,
             remainingRoutePolyline: MGLPolyline,
             routeCasingInitialLayerConfig: InitialLayerConfiguration,
             routeInitialLayerConfig: InitialLayerConfiguration,
             remainingRouteInitialLayerConfig: InitialLayerConfiguration) {
            self.routePolyline = routePolyline
            self.remainingRoutePolyline = remainingRoutePolyline

            self.routeCasingInitialLayerConfig = routeCasingInitialLayerConfig
            self.routeInitialLayerConfig = routeInitialLayerConfig
            self.remainingRouteInitialLayerConfig = remainingRouteInitialLayerConfig
        }

        func mapView(_ mapView: MGLMapView, didFinishLoading style: MGLStyle) {
            // Little dance: if we ever invoke
            // source.shape before the style is
            // loaded and the source is properly
            // added, we'll get a crash, and there's
            // no way to find out if this will happen,
            // so we err on the side of caution
            // and try our best to prevent this
            // with the type system + late init.
            //
            // Further, we create a new source each
            // time the style loads because adding
            // the same source to one map after
            // it's been added to another is also
            // an error and we can't guard against
            // it any other way.

            //
            // Full route
            //
            routeSource = MGLShapeSource(identifier: "full-route-polyline", shape: routePolyline)

            createLayer(id: "route-casing", style: style, source: routeSource!, config: routeCasingInitialLayerConfig)
            createLayer(id: "full-route", style: style, source: routeSource!, config: routeInitialLayerConfig)

            //
            // Remaining route
            //
            remainingRouteSource = MGLShapeSource(identifier: "remaining-route-polyline", shape: remainingRoutePolyline)

            createLayer(id: "remaining-route", style: style, source: remainingRouteSource!, config: remainingRouteInitialLayerConfig)
        }

        func updateStyleURL(_ url: URL, mapView: MGLMapView) {
            mapView.styleURL = url
        }

        private func createLayer(id: String, style: MGLStyle, source: MGLSource, config: InitialLayerConfiguration) {
            let newLayer = MGLLineStyleLayer(identifier: id, source: source)

            config.applyStyle?(newLayer)

            if style.source(withIdentifier: source.identifier) == nil {
                style.addSource(source)
            }


            switch config.position {
            case .above(layerId: let id):
                if let layer = style.layer(withIdentifier: id) {
                    style.insertLayer(newLayer, above: layer)
                } else {
                    NSLog("Failed to find layer with ID \(id). Adding layer on top.")
                    style.addLayer(newLayer)
                }
            case .below(layerId: let id):
                if let layer = style.layer(withIdentifier: id) {
                    style.insertLayer(newLayer, below: layer)
                } else {
                    NSLog("Failed to find layer with ID \(id). Adding layer on top.")
                    style.addLayer(newLayer)
                }
            case .onTopOfOthers:
                style.addLayer(newLayer)
            }
        }
    }

    func makeUIView(context: Context) -> MGLMapView {
        // Create the map view
        let mapView = MGLMapView(frame: .zero, styleURL: styleURL)
        mapView.delegate = context.coordinator

        mapView.logoView.isHidden = true

        mapView.setVisibleCoordinateBounds(routePolyline.overlayBounds, animated: false)

        return mapView
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(
            routePolyline: routePolyline,
            remainingRoutePolyline: remainingRoutePolyline,
            routeCasingInitialLayerConfig: routeCasingInitialLayerConfig,
            routeInitialLayerConfig: routeInitialLayerConfig,
            remainingRouteInitialLayerConfig: remainingRouteInitialLayerConfig)
        }

    func updateUIView(_ mapView: MGLMapView, context: Context) {
        context.coordinator.updateStyleURL(styleURL, mapView: mapView)
        context.coordinator.routePolyline = routePolyline
    }
}

struct MapView_Previews: PreviewProvider {
    static var previews: some View {
        MapView(
            styleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
            routePolyline: FerrostarObservableState.pedestrianExample.routePolyline,
            remainingRoutePolyline: FerrostarObservableState.pedestrianExample.remainingRoutePolyline,
            routeCasingInitialLayerConfig: InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
                newLayer.lineColor = NSExpression(forConstantValue: UIColor.white)
                let stops = NSExpression(forConstantValue: [14: 8,
                                                            18: 24])
                newLayer.lineWidth = NSExpression(forMGLInterpolating: .zoomLevelVariable,
                                                            curveType: .exponential,
                                                            parameters: NSExpression(forConstantValue: 1.5),
                                                            stops: stops)
                newLayer.lineCap = NSExpression(forConstantValue: "round")
                newLayer.lineJoin = NSExpression(forConstantValue: "round")
            },
            routeInitialLayerConfig: InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
                newLayer.lineColor = NSExpression(forConstantValue: UIColor.lightGray)
                let stops = NSExpression(forConstantValue: [14: 5,
                                                            18: 20])
                newLayer.lineWidth = NSExpression(forMGLInterpolating: .zoomLevelVariable,
                                                            curveType: .exponential,
                                                            parameters: NSExpression(forConstantValue: 1.5),
                                                            stops: stops)
                newLayer.lineCap = NSExpression(forConstantValue: "round")
                newLayer.lineJoin = NSExpression(forConstantValue: "round")
            },
            remainingRouteInitialLayerConfig: InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
                newLayer.lineColor = NSExpression(forConstantValue: UIColor.systemBlue)
                let stops = NSExpression(forConstantValue: [14: 5,
                                                            18: 20])
                newLayer.lineWidth = NSExpression(forMGLInterpolating: .zoomLevelVariable,
                                                            curveType: .exponential,
                                                            parameters: NSExpression(forConstantValue: 1.5),
                                                            stops: stops)
                newLayer.lineCap = NSExpression(forConstantValue: "round")
                newLayer.lineJoin = NSExpression(forConstantValue: "round")
            }
        )
        .edgesIgnoringSafeArea(.all)
    }
}
