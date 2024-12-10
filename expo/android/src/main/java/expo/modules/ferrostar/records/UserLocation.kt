package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable
import java.time.Instant

class UserLocation : Record, Serializable {
    @Field
    val coordinates: GeographicCoordinate = GeographicCoordinate()

    @Field
    val horizontalAccuracy: Double = 0.0

    @Field
    val courseOverGround: CourseOverGround? = null

    @Field
    val timestamp: String = Instant.now().toString()

    @Field
    val speed: Speed? = null

    fun toUserLocation(): uniffi.ferrostar.UserLocation {
        return uniffi.ferrostar.UserLocation(
            coordinates.toGeographicCoordinate(),
            horizontalAccuracy,
            courseOverGround?.toCourseOverGround(),
            Instant.parse(timestamp),
            speed?.toSpeed()
        )
    }
}