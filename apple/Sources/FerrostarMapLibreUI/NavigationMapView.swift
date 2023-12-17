import SwiftUI
import FerrostarCore
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI

public struct NavigationMapView: View {
    
    @Environment(\.colorScheme) var colorScheme
    
    let lightStyleURL: URL
    let darkStyleURL: URL

    var navigationState: FerrostarObservableState?

    @State private var camera: MapViewCamera

    public init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: FerrostarObservableState?,
        initialCamera: MapViewCamera = .backup()
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.navigationState = navigationState

        _camera = State(initialValue: initialCamera)

        // TODO: Set up following of the user
    }

    public var body: some View {
        MapView(
            styleURL: colorScheme == .dark ? darkStyleURL : lightStyleURL,
            camera: $camera
        )
        .routePolyline(navigationState?.routePolyline)
        .edgesIgnoringSafeArea(.all)
        .overlay(alignment: .top, content: {
            if let navigationState,
               let visualInstructions = navigationState.visualInstructions {
                BannerView(instructions: visualInstructions,
                           distanceToNextManeuver: navigationState.distanceToNextManeuver)
            }
        })
    }
}


struct NavigationView_Previews: PreviewProvider {
    // TODO: Move to environment
    private static let apiKey = "YOUR-API-KEY"
    static var previews: some View {
        NavigationMapView(
            lightStyleURL: URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(apiKey)")!,
            darkStyleURL: URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(apiKey)")!,
            navigationState: .modifiedPedestrianExample(droppingNWaypoints: 4)
        )
    }
}
