package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.NavigationController
import uniffi.ferrostar.RouteAdapter
import java.net.URL

// TODO: Network session
class FerrostarCore(val routeAdapter: RouteAdapter, val locationProvider: LocationProvider) {
    private var navigationController: NavigationController? = null

    constructor(
        valhallaEndpointURL: URL,
        profile: String,
        locationProvider: LocationProvider
    ) : this(
        RouteAdapter.newValhallaHttp(
            valhallaEndpointURL.toString(),
            profile
        ),
        locationProvider
    )
}
