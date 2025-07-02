import CoreLocation
import FerrostarCoreFFI
import Foundation

/// A Swift wrapper around `UniFFI.NavigationControllerConfig`.
public struct SwiftNavigationControllerConfig {
    public init(waypointAdvance: WaypointAdvanceMode,
                stepAdvanceCondition: StepAdvanceCondition,
                arrivalStepAdvanceCondition: StepAdvanceCondition,
                routeDeviationTracking: SwiftRouteDeviationTracking,
                snappedLocationCourseFiltering: CourseFiltering)
    {
        ffiValue = FerrostarCoreFFI.NavigationControllerConfig(
            waypointAdvance: waypointAdvance,
            stepAdvanceCondition: stepAdvanceCondition,
            arrivalStepAdvanceCondition: arrivalStepAdvanceCondition,
            routeDeviationTracking: routeDeviationTracking.ffiValue,
            snappedLocationCourseFiltering: snappedLocationCourseFiltering
        )
    }

    var ffiValue: FerrostarCoreFFI.NavigationControllerConfig
}
