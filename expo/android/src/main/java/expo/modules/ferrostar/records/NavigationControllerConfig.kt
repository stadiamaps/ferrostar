package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class NavigationControllerConfig : Record, Serializable {
    @Field
    val stepAdvance: RelativeLineStringDistance = RelativeLineStringDistance()

    @Field
    val routeDeviationTracking: StaticThreshold = StaticThreshold()

    @Field
    val snappedLocationCourseFiltering: CourseFiltering = CourseFiltering.SNAP_TO_ROUTE

    fun toConfig(): uniffi.ferrostar.NavigationControllerConfig {
        return uniffi.ferrostar.NavigationControllerConfig(
            stepAdvance = stepAdvance.toStepAdvanceMode(),
            routeDeviationTracking = routeDeviationTracking.toRouteDeviationTracking(),
            snappedLocationCourseFiltering = uniffi.ferrostar.CourseFiltering.valueOf(snappedLocationCourseFiltering.value)
        )
    }
}