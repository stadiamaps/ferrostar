import SwiftUI
import FerrostarCore
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI

struct NavigationMapView: View {
    let lightStyleURL: URL
    let darkStyleURL: URL

    var navigationState: FerrostarObservableState

    @State private var camera: MapView.Camera

    // TODO: Determine this automatically
    private var useDarkStyle: Bool {
        return false
    }

    init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: FerrostarObservableState
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        self.navigationState = navigationState

        _camera = State(initialValue: MapView.Camera.centerAndZoom(navigationState.fullRouteShape.first!, 14))

        // TODO: Set up following of the user
    }

    var body: some View {
        MapView(
            styleURL: useDarkStyle ? darkStyleURL : lightStyleURL,
            camera: $camera
        ) {
            let routePolylineSource = ShapeSource(identifier: "route-polyline-source") {
                navigationState.routePolyline
            }

            let remainingRoutePolylineSource = ShapeSource(identifier: "remaining-route-polyline-source") {
                navigationState.remainingRoutePolyline
            }

            let userLocationSource = ShapeSource(identifier: "user-location-source") {
                MLNPointFeature(coordinate: navigationState.snappedLocation.coordinate)
            }

            // TODO: Make this configurable via a modifier
            LineStyleLayer(identifier: "route-polyline-casing", source: routePolylineSource)
                .lineCap(constant: .round)
                .lineJoin(constant: .round)
                .lineColor(constant: .white)
                .lineWidth(interpolatedBy: .zoomLevel,
                           curveType: .exponential,
                           parameters: NSExpression(forConstantValue: 1.5),
                           stops: NSExpression(forConstantValue: [14: 6, 18: 24]))

            // TODO: Make this configurable via a modifier
            LineStyleLayer(identifier: "route-polyline", source: routePolylineSource)
                .lineCap(constant: .round)
                .lineJoin(constant: .round)
                .lineColor(constant: .lightGray)
                .lineWidth(interpolatedBy: .zoomLevel,
                           curveType: .exponential,
                           parameters: NSExpression(forConstantValue: 1.5),
                           stops: NSExpression(forConstantValue: [14: 3, 18: 16]))

            // TODO: Make this configurable via a modifier
            LineStyleLayer(identifier: "route-polyline-remaining", source: remainingRoutePolylineSource)
                .lineCap(constant: .round)
                .lineJoin(constant: .round)
                .lineColor(constant: .systemBlue)
                .lineWidth(interpolatedBy: .zoomLevel,
                           curveType: .exponential,
                           parameters: NSExpression(forConstantValue: 1.5),
                           stops: NSExpression(forConstantValue: [14: 3, 18: 16]))

            SymbolStyleLayer(identifier: "user-location", source: userLocationSource)
                .iconImage(constant: UIImage(systemName: "location.north.circle.fill")!)
                .iconRotation(constant: navigationState.heading?.trueHeading.magnitude ?? 0)
        }
        .edgesIgnoringSafeArea(.all)
        .overlay(alignment: .top, content: {
            if let visualInstructions = navigationState.visualInstructions {
                BannerView(instructions: visualInstructions)
            }
        })
    }
}


struct NavigationView_Previews: PreviewProvider {
    // TODO: Move to environment
    private static let apiKey = "e60944cf-3ccd-4fbc-892f-73f45da31486"
    static var previews: some View {
        NavigationMapView(
            lightStyleURL: URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(apiKey)")!,
            darkStyleURL: URL(string: "https://tiles.stadiamaps.com/styles/outdoors.json?api_key=\(apiKey)")!,
            navigationState: .modifiedPedestrianExample(droppingNWaypoints: 4)
        )
    }
}
