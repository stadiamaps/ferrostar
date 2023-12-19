import SwiftUI
import FerrostarCore
import MapLibre
import MapLibreSwiftDSL
import MapLibreSwiftUI

public struct NavigationMapView: View {
    
    @Environment(\.colorScheme) var colorScheme
    
    let lightStyleURL: URL
    let darkStyleURL: URL
    // TODO: Configurable camera and user "puck" rotation modes
    
    @State private var navigationState: FerrostarObservableState?
    private var previewRoutes: [Route]?
    @State private var camera: MapViewCamera

    /// Creates a new instance of the default navigation map.
    ///
    /// `lightStyleURL` and `darkStyleURL` are yet to be implemented.
    public init(
        lightStyleURL: URL,
        darkStyleURL: URL,
        navigationState: FerrostarObservableState?,
        previewRoutes routes: [Route]? = nil,
        initialCamera: MapViewCamera = .backup()
    ) {
        self.lightStyleURL = lightStyleURL
        self.darkStyleURL = darkStyleURL
        _navigationState = State(initialValue: navigationState)
//        _previewRoutes = State(initialValue: routes)
        previewRoutes = routes
        _camera = State(initialValue: initialCamera)

        // TODO: Set up following of the user
    }

    public var body: some View {
        MapView(
            styleURL: colorScheme == .dark ? darkStyleURL : lightStyleURL,
            camera: $camera
        )
        .mapOverlayRoute(previewRoutes?.first)
        // ) {
        //     let routePolylineSource = ShapeSource(identifier: "route-polyline-source") {
        //         navigationState.routePolyline
        //     }

        //     let remainingRoutePolylineSource = ShapeSource(identifier: "remaining-route-polyline-source") {
        //         navigationState.remainingRoutePolyline
        //     }

        //     let userLocationSource = ShapeSource(identifier: "user-location-source") {
        //         MLNPointFeature(coordinate: navigationState.snappedLocation.coordinate)
        //     }

        //     // TODO: Make this configurable via a modifier
        //     LineStyleLayer(identifier: "route-polyline-casing", source: routePolylineSource)
        //         .lineCap(constant: .round)
        //         .lineJoin(constant: .round)
        //         .lineColor(constant: .white)
        //         .lineWidth(interpolatedBy: .zoomLevel,
        //                    curveType: .exponential,
        //                    parameters: NSExpression(forConstantValue: 1.5),
        //                    stops: NSExpression(forConstantValue: [14: 6, 18: 24]))

        //     // TODO: Make this configurable via a modifier
        //     LineStyleLayer(identifier: "route-polyline", source: routePolylineSource)
        //         .lineCap(constant: .round)
        //         .lineJoin(constant: .round)
        //         .lineColor(constant: .lightGray)
        //         .lineWidth(interpolatedBy: .zoomLevel,
        //                    curveType: .exponential,
        //                    parameters: NSExpression(forConstantValue: 1.5),
        //                    stops: NSExpression(forConstantValue: [14: 3, 18: 16]))

        //     // TODO: Make this configurable via a modifier
        //     LineStyleLayer(identifier: "route-polyline-remaining", source: remainingRoutePolylineSource)
        //         .lineCap(constant: .round)
        //         .lineJoin(constant: .round)
        //         .lineColor(constant: .systemBlue)
        //         .lineWidth(interpolatedBy: .zoomLevel,
        //                    curveType: .exponential,
        //                    parameters: NSExpression(forConstantValue: 1.5),
        //                    stops: NSExpression(forConstantValue: [14: 3, 18: 16]))

        //     SymbolStyleLayer(identifier: "user-location", source: userLocationSource)
        //         .iconImage(constant: UIImage(systemName: "location.north.circle.fill")!)
        //         .iconRotation(constant: navigationState.courseOverGround?.magnitude ?? 0)
        // }
        .edgesIgnoringSafeArea(.all)
//        .overlay(alignment: .top, content: {
//            if let navigationState,
//               let visualInstructions = navigationState.visualInstructions {
//                BannerView(instructions: visualInstructions,
//                           distanceToNextManeuver: navigationState.distanceToNextManeuver)
//            }
//        })
    }
}


#Preview {
    // TODO: Make map URL configurable but gitignored
    return
        NavigationMapView(
            lightStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
            darkStyleURL: URL(string: "https://demotiles.maplibre.org/style.json")!,
            navigationState: .modifiedPedestrianExample(droppingNWaypoints: 4)
        )
}
