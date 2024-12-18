package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class CourseOverGround : Record, Serializable {
    @Field
    val degrees: Int = 0

    @Field
    val accuracy: Int? = null

    fun toCourseOverGround(): uniffi.ferrostar.CourseOverGround {
        return uniffi.ferrostar.CourseOverGround(degrees.toUShort(), accuracy?.toUShort())
    }
}