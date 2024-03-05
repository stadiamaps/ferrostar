<<<<<<< HEAD
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

import CoreLocation
import Foundation
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

    public func getPolyline(precision: UInt32) throws -> String {
        return try getRoutePolyline(route: inner, precision: precision)
    }
}

/// A Swift wrapper around `UniFFI.TripState`.
public enum TripState {
    case navigating(snappedUserLocation: CLLocation, remainingSteps: [UniFFI.RouteStep], remainingWaypoints: [CLLocationCoordinate2D], distanceToNextManeuver: CLLocationDistance, deviation: RouteDeviation)
    case complete

    init(_ update: UniFFI.TripState) {
        switch update {
        case let .navigating(snappedUserLocation: location, remainingSteps: remainingSteps, remainingWaypoints: remainingWaypoints, distanceToNextManeuver: distanceToNextManeuver, deviation: deviation):
            self = .navigating(snappedUserLocation: CLLocation(userLocation: location), remainingSteps: remainingSteps, remainingWaypoints: remainingWaypoints.map { coord in
                CLLocationCoordinate2D(geographicCoordinates: coord)
            }, distanceToNextManeuver: distanceToNextManeuver, deviation: deviation)
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
        case let .distanceToEndOfStep(distance: distance, minimumHorizontalAccuracy: minimumHorizontalAccuracy):
            return .distanceToEndOfStep(distance: distance, minimumHorizontalAccuracy: minimumHorizontalAccuracy)
        case let .relativeLineStringDistance(minimumHorizontalAccuracy: minimumHorizontalAccuracy, automaticAdvanceDistance: automaticAdvanceDistance):
            return .relativeLineStringDistance(minimumHorizontalAccuracy: minimumHorizontalAccuracy, automaticAdvanceDistance: automaticAdvanceDistance)
        }
=======
import CoreLocation
import FerrostarCoreFFI
import Foundation

public extension Route {
    func getPolyline(precision: UInt32) throws -> String {
        try getRoutePolyline(route: self, precision: precision)
>>>>>>> 746c43483e74319176f21e1fe96b78c038215c0b
    }
}

private class DetectorImpl: RouteDeviationDetector {
    let detectorFunc: (UserLocation, Route, RouteStep) -> RouteDeviation

    init(detectorFunc: @escaping (UserLocation, Route, RouteStep) -> RouteDeviation) {
        self.detectorFunc = detectorFunc
    }

    func checkRouteDeviation(location: UserLocation, route: Route, currentRouteStep: RouteStep) -> RouteDeviation {
        detectorFunc(location, route, currentRouteStep)
    }
}

/// A Swift wrapper around `UniFFI.RouteDeviationTracking`
public enum RouteDeviationTracking {
    case none

    case staticThreshold(minimumHorizontalAccuracy: UInt16, maxAcceptableDeviation: Double)

    case custom(detector: (UserLocation, Route, RouteStep) -> RouteDeviation)

    var ffiValue: FerrostarCoreFFI.RouteDeviationTracking {
        switch self {
        case .none:
<<<<<<< HEAD
            return .none
        case let .staticThreshold(minimumHorizontalAccuracy: minimumHorizontalAccuracy, maxAcceptableDeviation: maxAcceptableDeviation):
            return .staticThreshold(minimumHorizontalAccuracy: minimumHorizontalAccuracy, maxAcceptableDeviation: maxAcceptableDeviation)
        case let .custom(detector: detectorFunc):
            return .custom(detector: DetectorImpl(detectorFunc: detectorFunc))
=======
            .none
        case let .staticThreshold(
            minimumHorizontalAccuracy: minimumHorizontalAccuracy,
            maxAcceptableDeviation: maxAcceptableDeviation
        ):
            .staticThreshold(
                minimumHorizontalAccuracy: minimumHorizontalAccuracy,
                maxAcceptableDeviation: maxAcceptableDeviation
            )
        case let .custom(detector: detectorFunc):
            .custom(detector: DetectorImpl(detectorFunc: detectorFunc))
>>>>>>> 746c43483e74319176f21e1fe96b78c038215c0b
        }
    }
}

/// A Swift wrapper around `UniFFI.NavigationControllerConfig`.
public struct NavigationControllerConfig {
    public init(stepAdvance: StepAdvanceMode, routeDeviationTracking: RouteDeviationTracking) {
        ffiValue = FerrostarCoreFFI.NavigationControllerConfig(
            stepAdvance: stepAdvance,
            routeDeviationTracking: routeDeviationTracking.ffiValue
        )
    }

    var ffiValue: FerrostarCoreFFI.NavigationControllerConfig
}
