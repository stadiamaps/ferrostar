package expo.modules.ferrostar.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class Route : Record, Serializable {
    @Field
    var geometry: List<GeographicCoordinate> = emptyList()

    @Field
    var bbox: BoundingBox = BoundingBox()

    @Field
    var distance: Double = 0.0

    @Field
    var waypoints: List<Waypoint> = emptyList()

    @Field
    var steps: List<RouteStep> = emptyList()

    fun toRoute(): uniffi.ferrostar.Route {
        return uniffi.ferrostar.Route(
            geometry = geometry.map { it.toGeographicCoordinate() },
            bbox = bbox.toBoundingBox(),
            distance = distance,
            waypoints = waypoints.map { it.toWaypoint() },
            steps = steps.map { it.toRouteStep() }
        )
    }
}