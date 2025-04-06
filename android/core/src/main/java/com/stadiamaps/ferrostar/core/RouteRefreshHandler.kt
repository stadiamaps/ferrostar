package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.TripState

/** Handler for route refresh events. */
fun interface RouteRefreshHandler {
  /**
   * Called when a route refresh is needed
   *
   * @param core The Ferrostar core instance
   * @param tripState The current trip state
   * @return The corrective action to take
   */
  fun onRefreshNeeded(core: FerrostarCore, tripState: TripState): CorrectiveAction
}
