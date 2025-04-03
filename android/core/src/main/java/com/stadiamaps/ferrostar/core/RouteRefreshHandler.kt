package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.TripState

fun interface RouteRefreshHandler {

    fun onRefreshNeeded(
        core: FerrostarCore,
        tripState: TripState
    ): CorrectiveAction
}