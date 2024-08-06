package com.stadiamaps.ferrostar.core.extensions

import uniffi.ferrostar.Route
import uniffi.ferrostar.createRouteFromOsrm

/**
 * Create a [Route] from OSRM route and waypoint data.
 *
 * This behavior uses the same internal decoders as the OsrmResponseParser. This function will
 * automatically map & combine via and break waypoints from the route and waypoint data objects.
 *
 * @param route The encoded JSON data for the OSRM route.
 * @param waypoints The encoded JSON data for the OSRM waypoints.
 * @param polylinePrecision The polyline precision.
 * @return The navigation [Route]
 */
fun Route.Companion.fromOsrm(route: ByteArray, waypoints: ByteArray, polylinePrecision: UInt): Route {
    return createRouteFromOsrm(routeData = route, waypointData = waypoints, polylinePrecision)
}