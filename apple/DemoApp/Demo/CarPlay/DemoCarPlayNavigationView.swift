import FerrostarCarPlayUI
import FerrostarCore
import FerrostarMapLibreUI
import MapLibreSwiftUI
import SwiftUI

struct DemoCarPlayNavigationView: View {
    @Bindable var model: DemoCarPlayModel

    var body: some View {
        ZStack {
            if let errorMessage = model.errorMessage {
                ContentUnavailableView(
                    errorMessage, systemImage: "network.slash",
                    description: Text("error navigating.")
                )
            } else {
                CarPlayNavigationView(
                    styleURL: AppDefaults.mapStyleURL,
                    camera: $model.camera,
                    navigationState: model.coreState
                ) { _ in
                    if case let .routes(routes: routes) = model.appState {
                        for (idx, route) in routes.enumerated() {
                            RouteStyleLayer(polyline: route.polyline, identifier: "route-\(idx)")
                        }
                    }
                }
                .navigationSpeedLimit(
                    speedLimit: model.speedLimit,
                    speedLimitStyle: .mutcdStyle
                )
            }
        }
    }
}
