package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.types.Enumerable
import java.io.Serializable

class Waypoint : Record, Serializable {
    @Field
    var coordinate: GeographicCoordinate = GeographicCoordinate()

    @Field
    var kind: WaypointKind = WaypointKind.BREAK

    fun toWaypoint(): uniffi.ferrostar.Waypoint {
        return uniffi.ferrostar.Waypoint(coordinate.toGeographicCoordinate(), uniffi.ferrostar.WaypointKind.valueOf(kind.name))
    }
}