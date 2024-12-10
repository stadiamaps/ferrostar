package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class Speed : Record, Serializable {
    @Field
    val value: Double = 0.0

    @Field
    val accuracy: Double? = null

    fun toSpeed(): uniffi.ferrostar.Speed {
        return uniffi.ferrostar.Speed(value, accuracy)
    }
}