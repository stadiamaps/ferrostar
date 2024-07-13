package com.stadiamaps.ferrostar.support

import java.time.Instant
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.UserLocation

val initialSimulatedLocation =
    UserLocation(
        GeographicCoordinate(37.807770999999995, -122.41970699999999),
        6.0,
        null,
        Instant.now(),
        null)
