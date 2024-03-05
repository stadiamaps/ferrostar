package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.Route
import uniffi.ferrostar.getRoutePolyline

@Throws fun Route.getPolyline(precision: UInt): String = getRoutePolyline(this, precision)
