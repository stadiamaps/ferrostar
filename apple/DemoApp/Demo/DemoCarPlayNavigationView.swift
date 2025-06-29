import FerrostarCarPlayUI
import FerrostarCore
import MapLibreSwiftUI
import SwiftUI

struct DemoCarPlayNavigationView: View {
    var model: DemoModel
    let styleURL: URL

    var body: some View {
        @Bindable var bindableModel = model
        CarPlayNavigationView(navigationState: model.coreState, styleURL: styleURL, camera: $bindableModel.camera)
    }
}
