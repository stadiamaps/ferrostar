package com.stadiamaps.ferrostar

import com.stadiamaps.ferrostar.core.CorrectiveAction
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.FerrostarCoreDelegate
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.StepAdvanceMode

/**
 * Not all navigation apps will require a navigation delegate. In fact, we hope that most don't!
 * In case you do though, this sample implementation shows what you'll need to get started
 * by re-implementing the default behaviors of the core.
 */
class NavigationDelegate : FerrostarCoreDelegate {
    override fun correctiveActionForDeviation(
        core: FerrostarCore,
        deviationInMeters: Double,
        remainingWaypoints: List<GeographicCoordinate>
    ): CorrectiveAction = CorrectiveAction.GetNewRoutes(remainingWaypoints)

    override fun loadedAlternativeRoutes(core: FerrostarCore, routes: List<Route>) {
        // Automatically accepts the first new route if the framework was calculating a new route
        // due to the user being off course.
        // Pretty sensible default.
        if (core.isCalculatingNewRoute && routes.isNotEmpty()) {
            core.startNavigation(
                routes.first(), NavigationControllerConfig(
                    StepAdvanceMode.RelativeLineStringDistance(
                        minimumHorizontalAccuracy = 25U,
                        automaticAdvanceDistance = 10U
                    ),
                    RouteDeviationTracking.StaticThreshold(25U, 10.0)
                )
            )
        }
    }
}