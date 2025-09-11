import FerrostarCarPlayUI
import FerrostarCore
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
                    navigationCamera: .automotiveNavigation(zoom: 14),
                    navigationState: model.coreState
                )
                .navigationSpeedLimit(
                    // Configure speed limit signage based on user preference or location
                    speedLimit: model.core.annotation?.speedLimit,
                    speedLimitStyle: .mutcdStyle
                )
                .navigationMapViewContentInset(landscape: { proxy in
                    // TODO: This needs more testing on real car play screens.
                    //       If you come up with a solid content inset, feel free to post an issue.
                    .landscape(within: proxy, verticalPct: 0.7, horizontalPct: 0.95)
                })
            }
        }
    }
}
