import CoreLocation
import FerrostarCoreFFI
import Foundation

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
public enum SwiftRouteDeviationTracking {
    case none

    case staticThreshold(minimumHorizontalAccuracy: UInt16, maxAcceptableDeviation: Double)

    case custom(detector: (UserLocation, Route, RouteStep) -> RouteDeviation)

    var ffiValue: FerrostarCoreFFI.RouteDeviationTracking {
        switch self {
        case .none:
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
        }
    }
}

/// A Swift wrapper around `UniFFI.NavigationControllerConfig`.
public struct SwiftNavigationControllerConfig {
    public init(waypointAdvance: WaypointAdvanceMode,
                stepAdvance: StepAdvanceMode,
                routeDeviationTracking: SwiftRouteDeviationTracking,
                snappedLocationCourseFiltering: CourseFiltering)
    {
        ffiValue = FerrostarCoreFFI.NavigationControllerConfig(
            waypointAdvance: waypointAdvance,
            stepAdvance: stepAdvance,
            routeDeviationTracking: routeDeviationTracking.ffiValue,
            RouteRefreshStrategy: .routeRefreshStrategy.none,
            snappedLocationCourseFiltering: snappedLocationCourseFiltering
        )
    }

    var ffiValue: FerrostarCoreFFI.NavigationControllerConfig
}
