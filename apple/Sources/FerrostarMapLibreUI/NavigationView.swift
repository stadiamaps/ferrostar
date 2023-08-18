import SwiftUI
import FerrostarCore
import MapLibreSwiftUI

struct NavigationView: View {
    let lightStyleURL: URL
    let darkStyleURL: URL

    @ObservedObject var navigationState: FerrostarObservableState

    // TODO: Determine this automatically
    private var useDarkStyle: Bool {
        return false
    }

    init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: FerrostarObservableState
        // TODO: Remove once the DSL supports complex expressions
//        routeCasingInitialLayerConfig: InitialLayerConfiguration = InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
//            let stops = NSExpression(forConstantValue: [14: 6,
//                                                        18: 24])
//            newLayer.lineWidth = NSExpression(forMGLInterpolating: .zoomLevelVariable,
//                                                        curveType: .exponential,
//                                                        parameters: NSExpression(forConstantValue: 1.5),
//                                                        stops: stops)
//        },
//        routeInitialLayerConfig:  InitialLayerConfiguration = InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
//            let stops = NSExpression(forConstantValue: [14: 5,
//                                                        18: 20])
//            newLayer.lineWidth = NSExpression(forMGLInterpolating: .zoomLevelVariable,
//                                                        curveType: .exponential,
//                                                        parameters: NSExpression(forConstantValue: 1.5),
//                                                        stops: stops)
//        },
//        remainingRouteInitialLayerConfig: InitialLayerConfiguration = InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
//            let stops = NSExpression(forConstantValue: [14: 5,
//                                                        18: 20])
//            newLayer.lineWidth = NSExpression(forMGLInterpolating: .zoomLevelVariable,
//                                                        curveType: .exponential,
//                                                        parameters: NSExpression(forConstantValue: 1.5),
//                                                        stops: stops)
//        }
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.navigationState = navigationState
    }

    var body: some View {
        MapView(
            styleURL: useDarkStyle ? darkStyleURL : lightStyleURL
        ) {
            let routePolylineSource = ShapeSource(identifier: "route-polyline") {
                navigationState.routePolyline
            }

            let remainingRoutePolylineSource = ShapeSource(identifier: "remaining-route-polyline") {
                navigationState.remainingRoutePolyline
            }

            // TODO: Make this configurable via a modifier
            LineStyleLayer(identifier: "route-polyline-casing", source: routePolylineSource)
                .lineCap(constant: .round)
                .lineJoin(constant: .round)
                .lineColor(constant: .white)
                .lineWidth(constant: 8)

            // TODO: Make this configurable via a modifier
            LineStyleLayer(identifier: "route-polyline", source: routePolylineSource)
                .lineCap(constant: .round)
                .lineJoin(constant: .round)
                .lineColor(constant: .lightGray)
                .lineWidth(constant: 5)

            // TODO: Make this configurable via a modifier
            LineStyleLayer(identifier: "route-polyline-remaining", source: remainingRoutePolylineSource)
                .lineCap(constant: .round)
                .lineJoin(constant: .round)
                .lineColor(constant: .systemBlue)
                .lineWidth(constant: 5)

        }
        .initialCenter(center: navigationState.fullRouteShape.first!, zoom: 13)
        .edgesIgnoringSafeArea(.all)
    }
}


struct NavigationView_Previews: PreviewProvider {
    // TODO: Move to environment
    private static let apiKey = "YOUR-API-KEY"
    static var previews: some View {
        NavigationView(
            lightStyleURL: URL(string: "https://tiles.stadiamaps.com/styles/alidade_smooth.json?api_key=\(apiKey)")!,
            darkStyleURL: URL(string: "https://tiles.stadiamaps.com/styles/alidade_smooth_dark.json?api_key=\(apiKey)")!,
            navigationState: .modifiedPedestrianExample(droppingNWaypoints: 10)
        )
    }
}
