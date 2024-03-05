package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapterInterface
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint

sealed class RouteProvider {
  class RouteAdapter(val adapter: RouteAdapterInterface) : RouteProvider()

  class CustomProvider(val provider: CustomRouteProvider) : RouteProvider()
}

fun interface CustomRouteProvider {
  suspend fun getRoutes(userLocation: UserLocation, waypoints: List<Waypoint>): List<Route>
}
