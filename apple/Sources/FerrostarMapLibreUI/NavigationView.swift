import SwiftUI
import FerrostarCore

struct NavigationView: View {
    let lightStyleURL: URL
    let darkStyleURL: URL

    /// Inital layer configuration. Note that changing this during the view lifecycle generally has no effect.
    let routeCasingInitialLayerConfig: InitialLayerConfiguration
    /// Inital layer configuration. Note that changing this during the view lifecycle generally has no effect.
    let routeInitialLayerConfig: InitialLayerConfiguration
    /// Inital layer configuration. Note that changing this during the view lifecycle generally has no effect.
    let remainingRouteInitialLayerConfig: InitialLayerConfiguration

    @ObservedObject var navigationState: FerrostarObservableState

    // TODO: Determine this automatically
    private var useDarkStyle: Bool {
        return false
    }

    init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: FerrostarObservableState,
        routeCasingInitialLayerConfig: InitialLayerConfiguration = InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
            newLayer.lineColor = NSExpression(forConstantValue: UIColor.white)
            let stops = NSExpression(forConstantValue: [14: 6,
                                                        18: 24])
            newLayer.lineWidth = NSExpression(forMGLInterpolating: .zoomLevelVariable,
                                                        curveType: .exponential,
                                                        parameters: NSExpression(forConstantValue: 1.5),
                                                        stops: stops)
            newLayer.lineCap = NSExpression(forConstantValue: "round")
            newLayer.lineJoin = NSExpression(forConstantValue: "round")
        },
        routeInitialLayerConfig:  InitialLayerConfiguration = InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
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
        remainingRouteInitialLayerConfig: InitialLayerConfiguration = InitialLayerConfiguration(position: .onTopOfOthers) { newLayer in
            newLayer.lineColor = NSExpression(forConstantValue: UIColor.systemBlue)
            let stops = NSExpression(forConstantValue: [14: 5,
                                                        18: 20])
            newLayer.lineWidth = NSExpression(forMGLInterpolating: .zoomLevelVariable,
                                                        curveType: .exponential,
                                                        parameters: NSExpression(forConstantValue: 1.5),
                                                        stops: stops)
            newLayer.lineCap = NSExpression(forConstantValue: "round")
            newLayer.lineJoin = NSExpression(forConstantValue: "round")
        }) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.routeCasingInitialLayerConfig = routeCasingInitialLayerConfig
        self.routeInitialLayerConfig = routeInitialLayerConfig
        self.remainingRouteInitialLayerConfig = remainingRouteInitialLayerConfig
        self.navigationState = navigationState
    }

    var body: some View {
        MapView(
            styleURL: useDarkStyle ? darkStyleURL : lightStyleURL,
            routePolyline: navigationState.routePolyline,
            remainingRoutePolyline: navigationState.remainingRoutePolyline,
            routeCasingInitialLayerConfig: routeCasingInitialLayerConfig,
            routeInitialLayerConfig: routeInitialLayerConfig,
            remainingRouteInitialLayerConfig: remainingRouteInitialLayerConfig
        )
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
            navigationState: .pedestrianExample
        )
    }
}
