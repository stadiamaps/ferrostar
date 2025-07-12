import FerrostarCarPlayUI
import FerrostarCore
import MapLibreSwiftUI
import SwiftUI

struct DemoCarPlayNavigationView: View {
    @State var model: DemoCarPlayModel

    var body: some View {
        ZStack {
            if let errorMessage = model.errorMessage {
                ContentUnavailableView(
                    errorMessage, systemImage: "network.slash",
                    description: Text("error navigating.")
                )
            } else {
                @Bindable var bindableModel = model
                CarPlayNavigationView(
                    navigationState: model.coreState,
                    styleURL: AppDefaults.mapStyleURL,
                    camera: $bindableModel.camera
                )
            }
        }
    }
}
