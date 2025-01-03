package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class RelativeLineStringDistance : Record {
    @Field
    val minimumHorizontalAccuracy: Int = 0

    @Field
    val automaticAdvanceDistance: Int? = null

    fun toStepAdvanceMode(): uniffi.ferrostar.StepAdvanceMode {
        return uniffi.ferrostar.StepAdvanceMode.RelativeLineStringDistance(
            minimumHorizontalAccuracy = minimumHorizontalAccuracy.toUShort(),
            automaticAdvanceDistance = automaticAdvanceDistance?.toUShort()
        )
    }
}