package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.Waypoint

/** Corrective action to take when the user deviates from the route. */
sealed class CorrectiveAction {
  /**
   * Don't do anything.
   *
   * Note that this is most commonly paired with no route deviation tracking as a formality. Think
   * twice before using this as a mechanism for implementing your own logic outside of the provided
   * framework, as doing so will mean you miss out on state updates around alternate route
   * calculation.
   */
  object DoNothing : CorrectiveAction()

  /**
   * Tells the core to fetch new routes from the route adapter.
   *
   * Once they are available, the delegate will be notified of the new routes.
   */
  class GetNewRoutes(val waypoints: List<Waypoint>) : CorrectiveAction()
}

/** Reacts to the user deviating from a route, recommending a corrective action. */
fun interface RouteDeviationHandler {
  fun correctiveActionForDeviation(
      core: FerrostarCore,
      deviationInMeters: Double,
      remainingWaypoints: List<Waypoint>,
  ): CorrectiveAction
}
