package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.Route

/** Reacts to the core loading alternative routes. */
fun interface AlternativeRouteProcessor {
  /**
   * A hook for the developer can decide whether to act on new routes becoming available. This is
   * currently used for recalculation when the user diverges from the route, but can be extended for
   * other uses in the future.
   */
  fun loadedAlternativeRoutes(core: FerrostarCore, routes: List<Route>)
}
