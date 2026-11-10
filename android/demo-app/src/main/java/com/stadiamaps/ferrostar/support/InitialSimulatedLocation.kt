package com.stadiamaps.ferrostar.support

import com.stadiamaps.ferrostar.BuildConfig
import java.time.Instant
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.UserLocation

val initialSimulatedLocation =
    UserLocation(
        if (BuildConfig.UI_EXERCISE) GeographicCoordinate(40.775707, -74.031614)
        else GeographicCoordinate(37.807770999999995, -122.41970699999999),
        6.0,
        null,
        Instant.now(),
        null,
    )
