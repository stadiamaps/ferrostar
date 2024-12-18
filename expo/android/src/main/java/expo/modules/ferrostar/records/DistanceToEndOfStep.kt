package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

class DistanceToEndOfStep : Record {
    @Field
    val distance: Int = 0

    @Field
    val minimumHorizontalAccuracy: Int = 0
}