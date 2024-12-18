package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class GeographicCoordinate : Record, Serializable {
    @Field
    var lat: Double = 0.0

    @Field
    var lng: Double = 0.0

    fun toGeographicCoordinate(): uniffi.ferrostar.GeographicCoordinate {
        return uniffi.ferrostar.GeographicCoordinate(lat, lng)
    }
}