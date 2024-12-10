package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class StaticThreshold : Record {
    @Field
    val minimumHorizontalAccuracy: Int = 0

    @Field
    val maxAcceptableDeviation: Double = 0.0

    fun toRouteDeviationTracking(): uniffi.ferrostar.RouteDeviationTracking {
        return uniffi.ferrostar.RouteDeviationTracking.StaticThreshold(
            minimumHorizontalAccuracy = minimumHorizontalAccuracy.toUShort(),
            maxAcceptableDeviation = maxAcceptableDeviation
        )
    }
}