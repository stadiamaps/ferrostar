package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteAdapterInterface
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint

/** An abstraction around the various ways of getting routes. */
sealed class RouteProvider {
  /**
   * A route provider optimized for a request/response model such as HTTP or socket communications.
   */
  class RouteAdapter(val adapter: RouteAdapterInterface) : RouteProvider()

  /**
   * A provider commonly used for local route generation. Extensible for any sort of custom route
   * generation that doesn't fit the [RouteAdapter] use case.
   */
  class CustomProvider(val provider: CustomRouteProvider) : RouteProvider()
}

/**
 * A custom route provider is a generic asynchronous route generator.
 *
 * The typical use case for a custom route provider is local route generation, but it is generally
 * useful for any route generation that doesn't involve a standardized request generation (ex: HTTP
 * POST) -> request execution (eg: okhttp3) -> response parsing (stream of bytes) flow.
 *
 * This applies well to offline route generation, since you are not getting back a stream of bytes
 * (ex: from a socket) that need decoding, but rather a data structure from a function call which
 * just needs mapping into the Ferrostar route model.
 */
fun interface CustomRouteProvider {
  suspend fun getRoutes(userLocation: UserLocation, waypoints: List<Waypoint>): List<Route>
}
