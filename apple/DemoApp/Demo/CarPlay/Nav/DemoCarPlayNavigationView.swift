import FerrostarCarPlayUI
import FerrostarCore
import FerrostarCoreFFI
import FerrostarMapLibreUI
import FerrostarSwiftUI
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI
import SwiftUI

struct DemoCarPlayNavigationView: View {
    @Environment(\.carPlayNavController) var navController
    @Environment(\.colorScheme) var colorScheme
    @State var model = DemoCarPlayNavigationModel()

    var body: some View {
        CarPlayNavigationView(
            styleURL: AppDefaults.mapStyleURL,
            camera: $model.camera,
            navigationCamera: .automotiveNavigation(zoom: 15),
            navigationState: model.navigationState
        ) { _ in
            if case let .routes(routes: routes) = model.appState {
                for (idx, route) in routes.enumerated() {
                    RouteStyleLayer(polyline: route.polyline, identifier: "route-\(idx)")
                }
            }

            let source = ShapeSource(identifier: "userLocation") {
                // Demonstrate how to add a dynamic overlay;
                // also incidentally shows the extent of puck lag
                if let coordinate = model.lastCoordinate {
                    MLNPointFeature(coordinate: coordinate)
                }
            }

            CircleStyleLayer(identifier: "foo", source: source)
        }
        .navigationSpeedLimit(
            // Configure speed limit signage based on user preference or location
            speedLimit: model.speedLimit,
            speedLimitStyle: .mutcdStyle
        )
        .onAppear {
            Task {
                try? await model.onAppear(navController)
            }
        }
        .navigationBarHidden(true)
        .navigationBarBackButtonHidden(true)
    }
}
