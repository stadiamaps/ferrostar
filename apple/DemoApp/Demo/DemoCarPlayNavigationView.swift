import FerrostarCarPlayUI
import FerrostarCore
import MapLibreSwiftUI
import SwiftUI

struct DemoCarPlayNavigationView: View {
    @StateObject var ferrostarCore: FerrostarCore
    let styleURL: URL
    @Binding public var camera: MapViewCamera

    var body: some View {
        CarPlayNavigationView(navigationState: ferrostarCore.state, styleURL: styleURL, camera: $camera)
    }
}
