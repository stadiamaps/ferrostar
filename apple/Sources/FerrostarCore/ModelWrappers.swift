/// Various re-exported models.
///
/// This wrapper approach is unfortunaetly necessary beacuse Swift packages cannot yet
/// re-export inner modules. The types used in signatures have the information available, and values
/// returned from functions can be inspected, but the type name cannot be explicitly used in variable or
/// function signatures. So to work around the issue, we export a wrapper type that *can* be
/// referenced in other packages (like the UI) which need to hang on to the route without getting the whole
/// FFI.

import Foundation
import CoreLocation
import UniFFI

/// A wrapper around the FFI `Route`.
///
/// TODO: While other types in this file are mostly a nuisance, this one is downright problematic since
/// we will need to eventually make a good way to construct these for custom routing in app code.
/// See https://github.com/stadiamaps/ferrostar/issues/4.
public struct Route {
    let inner: UniFFI.Route

    var geometry: [CLLocationCoordinate2D] {
        inner.geometry.map { point in
            CLLocationCoordinate2D(geographicCoordinates: point)
        }
    }
}

/// A swifty `NavigationStateUpdate`.
public enum NavigationStateUpdate {
    case navigating(snappedUserLocation: CLLocation, remainingWaypoints: [CLLocationCoordinate2D], remainingSteps: [UniFFI.RouteStep], visualInstructions: UniFFI.VisualInstructions?, spokenInstruction: UniFFI.SpokenInstruction?)
    case arrived(visualInstructions: UniFFI.VisualInstructions?, spokenInstruction: UniFFI.SpokenInstruction?)

    init(_ update: UniFFI.NavigationStateUpdate) {
        switch (update) {
        case .navigating(snappedUserLocation: let location, remainingWaypoints: let waypoints, remainingSteps: let remainingSteps, visualInstructions: let visualInstructions, spokenInstruction: let spokenInstruction):
            self = .navigating(snappedUserLocation: CLLocation(userLocation: location), remainingWaypoints: waypoints.map { CLLocationCoordinate2D(geographicCoordinates: $0)}, remainingSteps: remainingSteps, visualInstructions: visualInstructions, spokenInstruction: spokenInstruction)
        case .arrived(visualInstructions: let visualInstructions, spokenInstruction: let spokenInstruction):
            self = .arrived(visualInstructions: visualInstructions, spokenInstruction: spokenInstruction)
        }
    }
}
