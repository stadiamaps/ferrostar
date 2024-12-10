package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class BoundingBox : Record, Serializable {
    @Field
    var sw: GeographicCoordinate = GeographicCoordinate()

    @Field
    var ne: GeographicCoordinate = GeographicCoordinate()

    fun toBoundingBox(): uniffi.ferrostar.BoundingBox {
        return uniffi.ferrostar.BoundingBox(sw.toGeographicCoordinate(), ne.toGeographicCoordinate())
    }
}