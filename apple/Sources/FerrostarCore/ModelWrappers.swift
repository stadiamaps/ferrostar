/// Various re-exported models.
///
/// This wrapper approach is unfortunaetly necessary beacuse Swift packages cannot yet
/// re-export inner modules. The types used in signatures have the information available, and values
/// returned from functions can be inspected, but the type name cannot be explicitly used in variable or
/// function signatures. So to work around the issue, we export a wrapper type that *can* be
/// referenced in other packages (like the UI) which need to hang on to the route without getting the whole
/// FFI.
///
/// This might be a candidate for macros once we have a few more examples.

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

    public var geometry: [CLLocationCoordinate2D] {
        inner.geometry.map { point in
            CLLocationCoordinate2D(geographicCoordinates: point)
        }
    }
}

/// A Swift wrapper around `UniFFI.TripState`.
public enum TripState {
    case navigating(snappedUserLocation: CLLocation, remainingSteps: [UniFFI.RouteStep], distanceToNextManeuver: Double)
    case complete

    init(_ update: UniFFI.TripState) {
        switch (update) {
        case .navigating(snappedUserLocation: let location, remainingSteps: let remainingSteps, distanceToNextManeuver: let distanceToNextManeuver):
            self = .navigating(snappedUserLocation: CLLocation(userLocation: location), remainingSteps: remainingSteps, distanceToNextManeuver: distanceToNextManeuver)
        case .complete:
            self = .complete
        }
    }
}

/// A Swift wrapper around `UniFFI.StepAdvanceMode`.
public enum StepAdvanceMode {
    /// Never advances to the next step automatically
    case manual
    /// Automatically advances when the user's location is close enough to the end of the step
    ///
    /// Distance and horizontal accuracy are measured  in meters.
    case distanceToEndOfStep(distance: UInt16, minimumHorizontalAccuracy: UInt16)
    /// At this (optional) distance, navigation should advance to the next step regardless
    /// of which LineString appears closer.
    case relativeLineStringDistance(minimumHorizontalAccuracy: UInt16, automaticAdvanceDistance: UInt16?)

    var ffiValue: UniFFI.StepAdvanceMode {
        switch self {
        case .manual:
            return .manual
        case .distanceToEndOfStep(distance: let distance, minimumHorizontalAccuracy: let minimumHorizontalAccuracy):
            return .distanceToEndOfStep(distance: distance, minimumHorizontalAccuracy: minimumHorizontalAccuracy)
        case .relativeLineStringDistance(minimumHorizontalAccuracy: let minimumHorizontalAccuracy, automaticAdvanceDistance: let automaticAdvanceDistance):
            return .relativeLineStringDistance(minimumHorizontalAccuracy: minimumHorizontalAccuracy, automaticAdvanceDistance: automaticAdvanceDistance)
        }
    }
}
